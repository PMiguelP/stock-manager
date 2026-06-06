package com.pelletsfactory.stock_manager.desktop.services;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SupportTicketSelectionService {
    private UUID selectedTicketId;

    public void select(UUID ticketId) {
        selectedTicketId = ticketId;
    }

    public UUID consume() {
        UUID selection = selectedTicketId;
        selectedTicketId = null;
        return selection;
    }
}
