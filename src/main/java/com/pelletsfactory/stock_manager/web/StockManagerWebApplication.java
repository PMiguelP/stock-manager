package com.pelletsfactory.stock_manager.web;

import com.pelletsfactory.stock_manager.StockManagerApplication;
import org.springframework.boot.SpringApplication;

public final class StockManagerWebApplication {

    private StockManagerWebApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(StockManagerApplication.class, args);
    }
}
