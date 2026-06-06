package com.pelletsfactory.stock_manager.web.controllers;

import com.pelletsfactory.stock_manager.common.services.TicketService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingPortalControllerTest {

    @Test
    void opensTicketUsingExplicitPortalRoute() {
        RecordingTicketService ticketService = new RecordingTicketService();
        TrackingPortalController controller = new TrackingPortalController(null, ticketService);
        UUID ticketId = UUID.randomUUID();

        String view = controller.abrirConversa("PEL-2026-001", ticketId);

        assertThat(view).isEqualTo("redirect:/tracking?code=PEL-2026-001&ticket=" + ticketId);
        assertThat(ticketService.codigoTracking).isEqualTo("PEL-2026-001");
        assertThat(ticketService.ticketId).isEqualTo(ticketId);
    }

    private static final class RecordingTicketService extends TicketService {
        private String codigoTracking;
        private UUID ticketId;

        private RecordingTicketService() {
            super(null, null, null, null, null);
        }

        @Override
        public com.pelletsfactory.stock_manager.common.dto.response.TicketDetailsDTO obterConversaCliente(
                String codigoTracking, UUID ticketId) {
            this.codigoTracking = codigoTracking;
            this.ticketId = ticketId;
            return null;
        }
    }
}
