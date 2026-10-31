package com.rasitbank.retrieval.pagination;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Sayfalı liste uçlarının ortak yanıtıdır: sayfadaki kayıtlar, sayfa numarası, boyutu ve toplamlar.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public PageResponse {
        content = List.copyOf(content);
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }
}
