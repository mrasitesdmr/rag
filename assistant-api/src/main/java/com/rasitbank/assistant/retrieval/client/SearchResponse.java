package com.rasitbank.assistant.retrieval.client;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * retrieval-api aramasının yanıtını taşır: en yakın parça önce olacak şekilde isabetler.
 */
public record SearchResponse(@Nullable List<SearchHit> hits) {
}
