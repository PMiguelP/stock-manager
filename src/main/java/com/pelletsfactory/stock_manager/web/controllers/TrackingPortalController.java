package com.pelletsfactory.stock_manager.web.controllers;

import com.pelletsfactory.stock_manager.web.services.TrackingPortalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class TrackingPortalController {

    private final TrackingPortalService trackingPortalService;

    public TrackingPortalController(TrackingPortalService trackingPortalService) {
        this.trackingPortalService = trackingPortalService;
    }

    @GetMapping({"/", "/tracking"})
    public String tracking(@RequestParam(required = false) String code, Model model) {
        String trackingCode = code == null ? "" : code.trim();
        model.addAttribute("trackingCode", trackingCode);

        if (!trackingCode.isBlank()) {
            trackingPortalService.findByTrackingCode(trackingCode)
                    .ifPresentOrElse(
                            order -> model.addAttribute("order", order),
                            () -> model.addAttribute("notFound", true)
                    );
        }

        return "tracking";
    }
}
