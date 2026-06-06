package com.pelletsfactory.stock_manager.web.controllers;

import com.pelletsfactory.stock_manager.common.dto.request.AbrirTicketRequestDTO;
import com.pelletsfactory.stock_manager.common.dto.request.ResponderTicketRequestDTO;
import com.pelletsfactory.stock_manager.common.services.TicketService;
import com.pelletsfactory.stock_manager.web.services.TrackingPortalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class TrackingPortalController {

    private final TrackingPortalService trackingPortalService;
    private final TicketService ticketService;

    public TrackingPortalController(TrackingPortalService trackingPortalService, TicketService ticketService) {
        this.trackingPortalService = trackingPortalService;
        this.ticketService = ticketService;
    }

    @GetMapping({"/", "/tracking"})
    public String tracking(@RequestParam(required = false) String code,
                           @RequestParam(required = false) UUID ticket,
                           Model model) {
        String trackingCode = code == null ? "" : code.trim();
        model.addAttribute("trackingCode", trackingCode);

        if (!trackingCode.isBlank()) {
            trackingPortalService.findByTrackingCode(trackingCode)
                    .ifPresentOrElse(
                            order -> {
                                model.addAttribute("order", order);
                                model.addAttribute("tickets", ticketService.listarTicketsDaEncomenda(trackingCode));
                                if (ticket != null) {
                                    model.addAttribute("selectedTicket", ticketService.obterConversaCliente(trackingCode, ticket));
                                }
                            },
                            () -> model.addAttribute("notFound", true)
                    );
        }

        return "tracking";
    }

    @PostMapping("/tracking/{codigo}/tickets")
    public String abrirTicket(@PathVariable String codigo,
                              @RequestParam String assunto,
                              @RequestParam String mensagem,
                              RedirectAttributes redirectAttributes) {
        try {
            var ticket = ticketService.abrirTicketComoCliente(codigo, new AbrirTicketRequestDTO(assunto, mensagem));
            redirectAttributes.addFlashAttribute("supportSuccess", "A sua conversa foi criada.");
            return redirect(codigo, ticket.id());
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("supportError", exception.getMessage());
            return redirect(codigo, null);
        }
    }

    @GetMapping("/tracking/{codigo}/tickets/{ticketId}")
    public String abrirConversa(@PathVariable String codigo,
                                @PathVariable UUID ticketId) {
        ticketService.obterConversaCliente(codigo, ticketId);
        return redirect(codigo, ticketId);
    }

    @PostMapping("/tracking/{codigo}/tickets/{ticketId}/messages")
    public String responderTicket(@PathVariable String codigo,
                                  @PathVariable UUID ticketId,
                                  @RequestParam String mensagem,
                                  RedirectAttributes redirectAttributes) {
        try {
            ticketService.responderComoCliente(codigo, ticketId, new ResponderTicketRequestDTO(mensagem));
            redirectAttributes.addFlashAttribute("supportSuccess", "Mensagem enviada.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("supportError", exception.getMessage());
        }
        return redirect(codigo, ticketId);
    }

    @GetMapping("/tracking/{codigo}/tickets/{ticketId}/messages-fragment")
    public String mensagensFragment(@PathVariable String codigo,
                                    @PathVariable UUID ticketId,
                                    Model model) {
        model.addAttribute("selectedTicket", ticketService.obterConversaCliente(codigo, ticketId));
        model.addAttribute("trackingCode", codigo);
        return "fragments/ticket-chat :: ticketChat";
    }

    private String redirect(String codigo, UUID ticketId) {
        String destination = "redirect:/tracking?code=" + codigo;
        return ticketId == null ? destination : destination + "&ticket=" + ticketId;
    }
}
