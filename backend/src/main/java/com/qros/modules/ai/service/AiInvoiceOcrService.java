package com.qros.modules.ai.service;

import static com.qros.modules.ai.support.AiJsonExtractor.isEnglish;
import static com.qros.modules.ai.support.AiJsonExtractor.normalizeLanguage;
import static com.qros.modules.ai.support.AiJsonExtractor.readStringList;
import static com.qros.modules.ai.support.AiJsonExtractor.readText;
import static com.qros.modules.ai.support.AiJsonExtractor.truncate;

import com.qros.modules.ai.dto.response.AiInvoiceOcrResponse;
import com.qros.modules.ai.dto.response.AiInvoiceOcrResponse.ExtractedInvoiceLine;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.support.AiJsonExtractor;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.ai.support.NoteSafetyAnalyzer;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.service.InventoryLookupService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import com.qros.shared.validation.ImageFileValidator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class AiInvoiceOcrService {

    private static final int MAX_CATALOG_ITEMS = 120;
    private static final int MAX_LINES = 50;
    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    private final ImageFileValidator imageFileValidator;
    private final InventoryLookupService inventoryLookupService;
    private final AiChatGateway aiChatGateway;
    private final AiPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final Counter aiRequestsCounter;

    public AiInvoiceOcrService(
            ImageFileValidator imageFileValidator,
            InventoryLookupService inventoryLookupService,
            AiChatGateway aiChatGateway,
            AiPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.imageFileValidator = imageFileValidator;
        this.inventoryLookupService = inventoryLookupService;
        this.aiChatGateway = aiChatGateway;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.aiRequestsCounter = Counter.builder("ai.requests.total")
                .tag("feature", "invoice-ocr")
                .description("AI supplier invoice vision OCR extraction requests")
                .register(meterRegistry);
    }

    public AiInvoiceOcrResponse parseInvoice(MultipartFile file, String language) {
        return parseInvoice(file, language, null);
    }

    public AiInvoiceOcrResponse parseInvoice(MultipartFile file, String language, Runnable quotaCheck) {
        String normLang = normalizeLanguage(language);
        LocalDateTime now = AppTime.now();

        if (file == null) {
            throw new BusinessException(ErrorCode.FILE_INVALID, "Image file cannot be empty");
        }
        imageFileValidator.validate(file);

        byte[] imageBytes;
        try {
            imageBytes = file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.FILE_INVALID, "Unable to read uploaded image file", ex);
        }

        if (quotaCheck != null) {
            quotaCheck.run();
        }
        String mimeType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "image/jpeg";

        List<InventoryItem> activeItems = inventoryLookupService.findActiveInventoryItemsOrdered();
        Map<Long, InventoryItem> activeById = activeItems.stream()
                .collect(Collectors.toMap(
                        InventoryItem::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        String catalogContext = buildCatalogContext(activeItems);
        String systemPrompt = promptBuilder.buildInvoiceOcrPrompt(catalogContext, normLang);
        String userPrompt = isEnglish(normLang)
                ? "Extract supplier details and all purchased inventory lines from this receipt."
                : "Đọc ảnh hóa đơn nhập hàng này và trích xuất đầy đủ các dòng nguyên liệu.";

        String rawReply = aiChatGateway.chatWithImage(userPrompt, systemPrompt, imageBytes, mimeType);
        aiRequestsCounter.increment();

        if (!AiJsonExtractor.isUsableModelOutput(rawReply)) {
            return buildFallbackResponse(normLang, rawReply, now);
        }

        AiInvoiceOcrResponse parsed = tryParseResponse(normLang, rawReply, activeItems, activeById, now);
        return parsed != null ? parsed : buildFallbackResponse(normLang, rawReply, now);
    }

    private AiInvoiceOcrResponse tryParseResponse(
            String language,
            String rawReply,
            List<InventoryItem> activeItems,
            Map<Long, InventoryItem> activeById,
            LocalDateTime generatedAt) {
        String extractedJson = AiJsonExtractor.extractJsonObject(rawReply);
        if (extractedJson == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(extractedJson);
            if (!root.isObject()) {
                return null;
            }

            String supplierName = normalizeNullableText(root.path("supplierName"), 150);
            String invoiceDate = normalizeNullableText(root.path("invoiceDate"), 30);
            String invoiceNumber = normalizeNullableText(root.path("invoiceNumber"), 80);
            BigDecimal totalAmount = readNonNegativeDecimal(root.path("totalAmount"), 2);
            List<String> warnings = new ArrayList<>(readStringList(root.path("warnings"), 5));

            JsonNode linesNode = root.path("lines");
            List<ExtractedInvoiceLine> extractedLines = new ArrayList<>();
            if (linesNode.isArray()) {
                for (JsonNode node : linesNode) {
                    ExtractedInvoiceLine line = buildLine(node, activeItems, activeById, language);
                    if (line != null) {
                        extractedLines.add(line);
                    }
                    if (extractedLines.size() >= MAX_LINES) {
                        break;
                    }
                }
            }

            if (extractedLines.isEmpty() && warnings.isEmpty()) {
                warnings.add(
                        isEnglish(language)
                                ? "No clear item lines detected on the receipt. Please add rows manually or upload a sharper photo."
                                : "Chưa nhận diện được dòng hàng rõ nét từ hóa đơn. Bạn có thể thêm dòng thủ công hoặc chụp ảnh rõ hơn.");
            }

            return new AiInvoiceOcrResponse(
                    language,
                    supplierName,
                    invoiceDate,
                    invoiceNumber,
                    totalAmount,
                    extractedLines,
                    warnings,
                    generatedAt,
                    true);
        } catch (Exception ex) {
            log.debug("Failed to parse Gemini Vision invoice OCR payload: {}", ex.getMessage());
            return null;
        }
    }

    private ExtractedInvoiceLine buildLine(
            JsonNode node, List<InventoryItem> activeItems, Map<Long, InventoryItem> activeById, String language) {
        String rawName = truncate(readText(node.path("rawName"), ""), 150);
        BigDecimal invoiceQuantity = readPositiveDecimal(node.path("quantity"), 3);
        if (rawName.isBlank() || invoiceQuantity == null) {
            return null;
        }

        String invoiceUnit = truncate(readText(node.path("unit"), ""), 30);
        BigDecimal lineTotal = readNonNegativeDecimal(node.path("lineTotal"), 2);
        BigDecimal invoiceUnitPrice = readNonNegativeDecimal(node.path("unitPrice"), 2);
        if ((invoiceUnitPrice == null || invoiceUnitPrice.compareTo(BigDecimal.ZERO) <= 0)
                && lineTotal != null
                && lineTotal.compareTo(BigDecimal.ZERO) > 0) {
            invoiceUnitPrice = lineTotal.divide(invoiceQuantity, 2, RoundingMode.HALF_UP);
        }
        if (lineTotal == null && invoiceUnitPrice != null) {
            lineTotal = invoiceUnitPrice.multiply(invoiceQuantity).setScale(2, RoundingMode.HALF_UP);
        }

        String confidence = normalizeConfidence(readText(node.path("confidence"), "MEDIUM"));
        String aiNotes = truncate(readText(node.path("notes"), ""), 120);

        long candidateId = node.path("matchedInventoryItemId").asLong(-1L);
        InventoryItem matchedItem = candidateId > 0 ? activeById.get(candidateId) : null;
        if (matchedItem == null) {
            matchedItem = findBestFuzzyMatch(rawName, activeItems);
        }

        if (matchedItem == null) {
            String note = !aiNotes.isBlank()
                    ? aiNotes
                    : (isEnglish(language)
                            ? "Not matched in inventory catalog — please select an item before importing."
                            : "Chưa khớp trong danh mục kho — vui lòng chọn nguyên liệu trước khi nhập.");
            return new ExtractedInvoiceLine(
                    rawName,
                    invoiceQuantity,
                    invoiceUnit,
                    invoiceUnitPrice,
                    lineTotal,
                    null,
                    null,
                    null,
                    null,
                    invoiceQuantity,
                    invoiceUnitPrice,
                    false,
                    false,
                    "UNMATCHED",
                    confidence,
                    note);
        }

        UnitConversionResult conversion =
                convertUnits(invoiceQuantity, invoiceUnitPrice, invoiceUnit, matchedItem.getUnit(), language);
        String matchStatus = conversion.unitMismatch() ? "UNIT_MISMATCH" : "MATCHED";
        String combinedNote = combineNotes(conversion.note(), aiNotes);

        return new ExtractedInvoiceLine(
                rawName,
                invoiceQuantity,
                invoiceUnit,
                invoiceUnitPrice,
                lineTotal,
                matchedItem.getId(),
                matchedItem.getName(),
                matchedItem.getUnit(),
                matchedItem.getUnitCost() != null
                        ? matchedItem.getUnitCost().setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                conversion.convertedQuantity(),
                conversion.convertedUnitCost(),
                conversion.unitConverted(),
                conversion.unitMismatch(),
                matchStatus,
                confidence,
                combinedNote);
    }

    static UnitConversionResult convertUnits(
            BigDecimal quantity, BigDecimal unitPrice, String fromUnit, String toUnit, String language) {
        BigDecimal safeQty = quantity.setScale(3, RoundingMode.HALF_UP);
        BigDecimal safePrice = unitPrice != null ? unitPrice.setScale(2, RoundingMode.HALF_UP) : null;

        String normFrom = normalizeUnitCode(fromUnit);
        String normTo = normalizeUnitCode(toUnit);

        if (normTo.isBlank() || normFrom.equals(normTo)) {
            return new UnitConversionResult(safeQty, safePrice, false, false, "");
        }

        BigDecimal factor = null;
        if ("kg".equals(normFrom) && "g".equals(normTo)) {
            factor = THOUSAND;
        } else if ("g".equals(normFrom) && "kg".equals(normTo)) {
            factor = BigDecimal.ONE.divide(THOUSAND, 6, RoundingMode.HALF_UP);
        } else if ("l".equals(normFrom) && "ml".equals(normTo)) {
            factor = THOUSAND;
        } else if ("ml".equals(normFrom) && "l".equals(normTo)) {
            factor = BigDecimal.ONE.divide(THOUSAND, 6, RoundingMode.HALF_UP);
        }

        if (factor != null) {
            BigDecimal convertedQty = quantity.multiply(factor).setScale(3, RoundingMode.HALF_UP);
            if (convertedQty.compareTo(BigDecimal.ZERO) <= 0) {
                String tinyNote = isEnglish(language)
                        ? "Converted quantity (%s %s -> %s) rounds below 0.001 — please verify manually."
                                .formatted(safeQty.stripTrailingZeros().toPlainString(), fromUnit, toUnit)
                        : "Số lượng sau quy đổi (%s %s -> %s) nhỏ hơn 0.001 — vui lòng nhập thủ công."
                                .formatted(safeQty.stripTrailingZeros().toPlainString(), fromUnit, toUnit);
                return new UnitConversionResult(safeQty, safePrice, false, true, tinyNote);
            }
            BigDecimal convertedCost = unitPrice != null ? unitPrice.divide(factor, 2, RoundingMode.HALF_UP) : null;
            String note = isEnglish(language)
                    ? "Auto-converted %s %s -> %s %s"
                            .formatted(
                                    safeQty.stripTrailingZeros().toPlainString(),
                                    fromUnit,
                                    convertedQty.stripTrailingZeros().toPlainString(),
                                    toUnit)
                    : "Tự quy đổi %s %s -> %s %s"
                            .formatted(
                                    safeQty.stripTrailingZeros().toPlainString(),
                                    fromUnit,
                                    convertedQty.stripTrailingZeros().toPlainString(),
                                    toUnit);
            return new UnitConversionResult(convertedQty, convertedCost, true, false, note);
        }

        String mismatchNote = isEnglish(language)
                ? "Receipt unit '%s' differs from stock unit '%s' — please verify converted quantity."
                        .formatted(fromUnit == null || fromUnit.isBlank() ? "?" : fromUnit, toUnit)
                : "Đơn vị hóa đơn '%s' khác đơn vị kho '%s' — hãy kiểm tra lại số lượng quy đổi."
                        .formatted(fromUnit == null || fromUnit.isBlank() ? "?" : fromUnit, toUnit);
        return new UnitConversionResult(safeQty, safePrice, false, true, mismatchNote);
    }

    static String normalizeUnitCode(String rawUnit) {
        if (rawUnit == null || rawUnit.isBlank()) {
            return "";
        }
        String folded = NoteSafetyAnalyzer.foldAccents(rawUnit).replace(".", "").trim();
        return switch (folded) {
            case "kg", "kgs", "kilogram", "kilograms", "kilo", "ki", "ky" -> "kg";
            case "g", "gr", "gram", "grams", "gam" -> "g";
            case "l", "lit", "liter", "liters", "litre", "litres" -> "l";
            case "ml", "mililit", "milliliter", "milliliters", "millilitre", "cc" -> "ml";
            default -> folded;
        };
    }

    static InventoryItem findBestFuzzyMatch(String rawName, List<InventoryItem> activeItems) {
        if (rawName == null || rawName.isBlank() || activeItems == null || activeItems.isEmpty()) {
            return null;
        }
        String foldedRaw = NoteSafetyAnalyzer.foldAccents(rawName);
        if (foldedRaw.isBlank()) {
            return null;
        }
        Set<String> rawTokens = tokenize(foldedRaw);

        InventoryItem best = null;
        double bestScore = 0.0;

        for (InventoryItem candidate : activeItems) {
            String foldedCandidate = NoteSafetyAnalyzer.foldAccents(candidate.getName());
            if (foldedCandidate.equals(foldedRaw)) {
                return candidate;
            }

            Set<String> candidateTokens = tokenize(foldedCandidate);
            if (rawTokens.isEmpty() || candidateTokens.isEmpty()) {
                continue;
            }

            long intersection =
                    rawTokens.stream().filter(candidateTokens::contains).count();
            if (intersection == 0) {
                continue;
            }

            // Require whole-token subset match with at least 2 matching tokens (or single-token exact equality)
            // so short words like "bo" or "ca" never false-match "thit bo uc" or "ca chua".
            boolean candidateSubsetOfRaw = candidateTokens.size() >= 2 && rawTokens.containsAll(candidateTokens);
            boolean rawSubsetOfCandidate = rawTokens.size() >= 2 && candidateTokens.containsAll(rawTokens);

            double score;
            if (candidateSubsetOfRaw || rawSubsetOfCandidate) {
                int smaller = Math.min(rawTokens.size(), candidateTokens.size());
                int larger = Math.max(rawTokens.size(), candidateTokens.size());
                score = 0.75 + 0.15 * ((double) smaller / larger);
            } else {
                long union = rawTokens.size() + candidateTokens.size() - intersection;
                score = (double) intersection / union;
            }

            if (score >= 0.5 && score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private AiInvoiceOcrResponse buildFallbackResponse(String language, String rawReply, LocalDateTime generatedAt) {
        String warning = isEnglish(language)
                ? "AI Vision invoice scanner is currently unavailable or could not read this image clearly. You can still add stock lines manually below."
                : "AI Vision hiện chưa khả dụng hoặc chưa đọc rõ ảnh hóa đơn này. Bạn vẫn có thể thêm dòng nhập kho thủ công bên dưới.";
        if (rawReply != null && rawReply.startsWith("Tính năng trợ lý AI") && !isEnglish(language)) {
            warning = rawReply + " Bạn vẫn có thể thêm dòng nhập kho thủ công bên dưới.";
        }
        return new AiInvoiceOcrResponse(
                language, null, null, null, null, List.of(), List.of(warning), generatedAt, false);
    }

    private String buildCatalogContext(List<InventoryItem> activeItems) {
        if (activeItems.isEmpty()) {
            return "- NONE\n";
        }
        StringBuilder sb = new StringBuilder();
        activeItems.stream()
                .limit(MAX_CATALOG_ITEMS)
                .forEach(item -> sb.append("- id=")
                        .append(item.getId())
                        .append(" | name=")
                        .append(item.getName())
                        .append(" | unit=")
                        .append(item.getUnit())
                        .append(" | unitCost=")
                        .append(item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO)
                        .append('\n'));
        return sb.toString();
    }

    private static Set<String> tokenize(String foldedText) {
        return Arrays.stream(foldedText.replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(t -> t.length() >= 2)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalizeNullableText(JsonNode node, int maxLen) {
        String text = readText(node, "");
        if (text.isBlank() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        return truncate(text, maxLen);
    }

    private static String normalizeConfidence(String raw) {
        String upper = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if ("HIGH".equals(upper) || "MEDIUM".equals(upper) || "LOW".equals(upper)) {
            return upper;
        }
        return "MEDIUM";
    }

    private static BigDecimal readPositiveDecimal(JsonNode node, int scale) {
        return AiJsonExtractor.readPositiveDecimal(node, scale);
    }

    private static BigDecimal readNonNegativeDecimal(JsonNode node, int scale) {
        return AiJsonExtractor.readNonNegativeDecimal(node, scale);
    }

    private static String combineNotes(String first, String second) {
        if (first != null && !first.isBlank() && second != null && !second.isBlank()) {
            return truncate(first + " · " + second, 180);
        }
        if (first != null && !first.isBlank()) {
            return truncate(first, 180);
        }
        return truncate(second, 180);
    }

    record UnitConversionResult(
            BigDecimal convertedQuantity,
            BigDecimal convertedUnitCost,
            boolean unitConverted,
            boolean unitMismatch,
            String note) {}
}
