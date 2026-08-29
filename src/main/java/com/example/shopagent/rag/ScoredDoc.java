package com.example.shopagent.rag;

import java.util.Map;

/**
 * A retrieved document with its similarity score.
 *
 * @param id       document identifier (e.g. FAQ entry id such as {@code F001})
 * @param text     the document content used for retrieval / grounding
 * @param score    similarity score, higher means more relevant
 * @param metadata arbitrary extra fields (category, question, answer, ...)
 */
public record ScoredDoc(String id, String text, double score, Map<String, Object> metadata) {}
