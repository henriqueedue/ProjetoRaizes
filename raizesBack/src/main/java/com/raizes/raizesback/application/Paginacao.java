package com.raizes.raizesback.application;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Converte page (base 1) e limit da API para Pageable (limit maximo = 100). */
public final class Paginacao {
    private Paginacao() {}

    public static Pageable de(int page, int limit) {
        return de(page, limit, Sort.unsorted());
    }

    public static Pageable de(int page, int limit, Sort sort) {
        return PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(limit, 1), 100), sort);
    }
}
