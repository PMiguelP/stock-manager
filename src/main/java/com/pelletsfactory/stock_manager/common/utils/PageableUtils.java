package com.pelletsfactory.stock_manager.common.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableUtils {

    private static final int MAX_PAGE_SIZE = 100;

    private PageableUtils() {
    }

    public static Pageable create(int page, int pageSize, String sortBy, String direction, String defaultSort) {
        if (page < 1) {
            throw new IllegalArgumentException("Página deve ser maior ou igual a 1");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Tamanho da página deve estar entre 1 e " + MAX_PAGE_SIZE);
        }
        String property = sortBy == null || sortBy.isBlank() ? defaultSort : sortBy;
        if (property == null || !property.matches("[A-Za-z][A-Za-z0-9.]*")) {
            throw new IllegalArgumentException("Campo de ordenação inválido");
        }
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page - 1, pageSize, Sort.by(sortDirection, property));
    }
}
