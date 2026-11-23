package com.qros.modules.ai.support;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Extracts and safely reads structured JSON payloads from raw LLM text responses
 * that may include markdown code fences or wrapping prose.
 */
public final class AiJsonExtractor {

    private static final String FALLBACK_PREFIX = "Xin lỗi";
    private static final String CONFIG_PREFIX = "Tính năng trợ lý";

    private AiJsonExtractor() {}

    public static boolean isUsableModelOutput(String reply) {
        return reply != null
                && !reply.isBlank()
                && !reply.startsWith(FALLBACK_PREFIX)
                && !reply.startsWith(CONFIG_PREFIX);
    }

    public static String extractJsonObject(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }
        String candidate = rawText.trim();
        int jsonFenceStart = candidate.indexOf("```json");
        if (jsonFenceStart >= 0) {
            int jsonFenceEnd = candidate.indexOf("```", jsonFenceStart + 7);
            if (jsonFenceEnd > jsonFenceStart) {
                candidate =
                        candidate.substring(jsonFenceStart + 7, jsonFenceEnd).trim();
            }
        } else {
            int plainFenceStart = candidate.indexOf("```");
            if (plainFenceStart >= 0) {
                int plainFenceEnd = candidate.indexOf("```", plainFenceStart + 3);
                if (plainFenceEnd > plainFenceStart) {
                    candidate = candidate
                            .substring(plainFenceStart + 3, plainFenceEnd)
                            .trim();
                }
            }
        }

        int firstBrace = candidate.indexOf('{');
        int lastBrace = candidate.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return candidate.substring(firstBrace, lastBrace + 1);
        }
        return null;
    }

    public static String normalizeLanguage(String language) {
        return "en".equalsIgnoreCase(language) ? "en" : "vi";
    }

    public static boolean isEnglish(String language) {
        return "en".equalsIgnoreCase(language);
    }

    public static String readText(JsonNode node, String fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        String value = node.asString(fallback);
        return value != null ? value.trim() : fallback;
    }

    public static List<String> readStringList(JsonNode node, int limit) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (JsonNode child : node) {
            if (child != null && !child.isNull()) {
                String text = child.asString("").trim();
                if (!text.isBlank()) {
                    list.add(text);
                }
            }
            if (list.size() >= limit) {
                break;
            }
        }
        return list;
    }

    public static String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen).trim();
    }
}
