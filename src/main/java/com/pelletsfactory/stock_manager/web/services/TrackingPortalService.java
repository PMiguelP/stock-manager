package com.pelletsfactory.stock_manager.web.services;

import com.pelletsfactory.stock_manager.common.entities.EncomendaCliente;
import com.pelletsfactory.stock_manager.common.entities.ItemEncomendaCliente;
import com.pelletsfactory.stock_manager.common.enums.EstadoEncomendaCliente;
import com.pelletsfactory.stock_manager.common.repositories.EncomendaClienteRepository;
import com.pelletsfactory.stock_manager.common.repositories.ItemEncomendaClienteRepository;
import com.pelletsfactory.stock_manager.web.dto.TrackingOrderView;
import com.pelletsfactory.stock_manager.web.dto.TrackingItemView;
import com.pelletsfactory.stock_manager.web.dto.TrackingStageView;
import org.springframework.stereotype.Service;
import com.pelletsfactory.stock_manager.common.utils.CalculationUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
public class TrackingPortalService {

    private static final List<StageDefinition> STAGES = List.of(
            new StageDefinition("Pending", "Your order has been received"),
            new StageDefinition("In Production", "Your pellets are currently being produced"),
            new StageDefinition("Ready for Shipment", "Your order is prepared for dispatch"),
            new StageDefinition("Shipped", "Your order has left the factory")
    );
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final EncomendaClienteRepository orderRepository;
    private final ItemEncomendaClienteRepository itemRepository;

    public TrackingPortalService(EncomendaClienteRepository orderRepository,
                                 ItemEncomendaClienteRepository itemRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
    }

    public Optional<TrackingOrderView> findByTrackingCode(String trackingCode) {
        String normalizedCode = normalize(trackingCode);
        if (normalizedCode == null) {
            return Optional.empty();
        }

        return orderRepository.findByCodigoTrackingIgnoreCase(normalizedCode)
                .map(this::toView);
    }

    private TrackingOrderView toView(EncomendaCliente order) {
        List<ItemEncomendaCliente> items = itemRepository.findByEncomendaId(order.getId());
        double quantityKg = items.stream()
                .map(ItemEncomendaCliente::getQuantidadeKg)
                .filter(quantity -> quantity != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        List<TrackingItemView> itemViews = items.stream()
                .map(this::toItemView)
                .toList();

        EstadoEncomendaCliente status = order.getEstado();
        boolean cancelled = EstadoEncomendaCliente.CANCELADA.equals(status);

        return new TrackingOrderView(
                shortReference(order),
                order.getCliente() != null ? order.getCliente().getNome() : "Client unavailable",
                quantityKg,
                order.getData(),
                formatUpdatedAt(order),
                statusLabel(status),
                statusClass(status),
                order.getCodigoTracking(),
                order.getMoeda() != null ? order.getMoeda().getCodigo() : "",
                order.getMoeda() != null ? order.getMoeda().getSimbolo() : "",
                valueOrZero(order.getTotalNet()),
                valueOrZero(order.getTotalIva()),
                valueOrZero(order.getTotalFinal()),
                cancelled,
                buildStages(status),
                itemViews
        );
    }

    private TrackingItemView toItemView(ItemEncomendaCliente item) {
        double quantityKg = valueOrZero(item.getQuantidadeKg());
        double unitPriceNet = valueOrZero(item.getPrecoUnitarioNet());
        double vatAmount = valueOrZero(item.getValorIvaCalculado());
        double subtotalNet = CalculationUtils.subtotal(quantityKg, unitPriceNet);

        return new TrackingItemView(
                item.getTipoPellet() != null ? item.getTipoPellet().getNome() : "Pellet",
                quantityKg,
                unitPriceNet,
                valueOrZero(item.getTaxaIva()),
                vatAmount,
                subtotalNet,
                CalculationUtils.total(subtotalNet, vatAmount)
        );
    }

    private String formatUpdatedAt(EncomendaCliente order) {
        if (order.getUpdatedAt() != null) {
            return DATE_TIME_FORMATTER.format(order.getUpdatedAt());
        }
        if (order.getCreatedAt() != null) {
            return DATE_TIME_FORMATTER.format(order.getCreatedAt());
        }
        return order.getData() != null ? order.getData().toString() : "";
    }

    private double valueOrZero(Double value) {
        return value != null ? value : 0.0;
    }

    private List<TrackingStageView> buildStages(EstadoEncomendaCliente status) {
        int currentStage = currentStage(status);
        return IntStream.range(0, STAGES.size())
                .mapToObj(index -> {
                    StageDefinition stage = STAGES.get(index);
                    String state = index < currentStage ? "complete"
                            : index == currentStage ? "current"
                            : "upcoming";
                    return new TrackingStageView(stage.title(), stage.description(), state);
                })
                .toList();
    }

    private int currentStage(EstadoEncomendaCliente status) {
        if (status == null || EstadoEncomendaCliente.CANCELADA.equals(status)) {
            return -1;
        }
        return switch (status) {
            case PENDENTE, CONFIRMADA -> 0;
            case EM_PRODUCAO -> 1;
            case PRONTA -> 2;
            case EXPEDIDA -> 3;
            case CANCELADA -> -1;
        };
    }

    private String statusLabel(EstadoEncomendaCliente status) {
        if (status == null) {
            return "Unknown";
        }
        return switch (status) {
            case PENDENTE -> "Pending";
            case CONFIRMADA -> "Confirmed";
            case EM_PRODUCAO -> "In Production";
            case PRONTA -> "Ready for Shipment";
            case EXPEDIDA -> "Shipped";
            case CANCELADA -> "Cancelled";
        };
    }

    private String statusClass(EstadoEncomendaCliente status) {
        if (status == null) {
            return "neutral";
        }
        return switch (status) {
            case PENDENTE, CONFIRMADA -> "pending";
            case EM_PRODUCAO -> "production";
            case PRONTA -> "ready";
            case EXPEDIDA -> "shipped";
            case CANCELADA -> "cancelled";
        };
    }

    private String shortReference(EncomendaCliente order) {
        String id = order.getId().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return "ORD-" + id.substring(0, 8);
    }

    private String normalize(String trackingCode) {
        if (trackingCode == null || trackingCode.isBlank()) {
            return null;
        }
        return trackingCode.trim().toUpperCase(Locale.ROOT);
    }

    private record StageDefinition(String title, String description) {
    }
}
