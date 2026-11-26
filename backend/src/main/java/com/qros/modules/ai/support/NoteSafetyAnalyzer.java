package com.qros.modules.ai.support;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deterministic Layer-1 safety classifier for customer kitchen notes and table service requests.
 * Combines tone-preserved Vietnamese matching for ambiguous single-syllable food nouns
 * with accent-folded phrase matching (including đ/Đ) to avoid false positives on everyday words
 * such as "của", "ghế", "số", "sửa", "mẹ", "với".
 */
public final class NoteSafetyAnalyzer {

    public static final String RISK_CRITICAL = "CRITICAL";
    public static final String RISK_HIGH = "HIGH";
    public static final String RISK_NORMAL = "NORMAL";

    private static final Locale VI_LOCALE = Locale.forLanguageTag("vi");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern UNICODE_NON_WORD = Pattern.compile("[^\\p{L}\\p{N}\\s]+");

    private record RuleDefinition(String code, List<String> foldedKeywords, List<String> rawKeywords) {}

    private static final List<RuleDefinition> ALLERGEN_RULES = List.of(
            new RuleDefinition(
                    "ALLERGY",
                    List.of("di ung", "allergy", "allergic", "anaphylaxis", "soc phan ve"),
                    List.of("dị ứng")),
            new RuleDefinition(
                    "PEANUT_NUTS",
                    List.of(
                            "dau phong",
                            "cu lac",
                            "dau lac",
                            "hat dieu",
                            "hanh nhan",
                            "peanut",
                            "peanuts",
                            "almond",
                            "cashew",
                            "walnut"),
                    List.of("đậu phộng", "lạc", "hạt điều", "hạnh nhân")),
            new RuleDefinition(
                    "SEAFOOD",
                    List.of(
                            "hai san",
                            "seafood",
                            "shellfish",
                            "tom hum",
                            "tom su",
                            "nuoc mam",
                            "mam tom",
                            "shrimp",
                            "prawn",
                            "crab",
                            "lobster",
                            "squid",
                            "oyster"),
                    List.of("hải sản", "tôm", "cua", "mực", "ghẹ", "sò", "ngao", "nghêu", "ốc", "hến")),
            new RuleDefinition(
                    "GLUTEN",
                    List.of("gluten", "celiac", "lua mi", "bot mi", "wheat", "barley"),
                    List.of("lúa mì", "bột mì")),
            new RuleDefinition(
                    "DAIRY",
                    List.of("sua bo", "sua tuoi", "sua dac", "lactose", "dairy", "pho mai", "milk", "cheese", "butter"),
                    List.of("sữa", "phô mai")),
            new RuleDefinition(
                    "EGG",
                    List.of("trung ga", "trung vit", "trung cut", "long trang", "long do", "egg", "eggs"),
                    List.of("trứng")),
            new RuleDefinition("MSG", List.of("mi chinh", "bot ngot", "msg"), List.of("mì chính", "bột ngọt")),
            new RuleDefinition(
                    "SESAME_SOY",
                    List.of(
                            "me den",
                            "me rang",
                            "hat me",
                            "hat vung",
                            "dau vung",
                            "dau nanh",
                            "tuong nanh",
                            "sesame",
                            "soy",
                            "soybean",
                            "tofu"),
                    List.of("vừng", "mè", "đậu nành")));

    private static final List<RuleDefinition> URGENCY_RULES = List.of(
            new RuleDefinition(
                    "URGENT",
                    List.of(
                            "can gap",
                            "lam gap",
                            "nau gap",
                            "lam nhanh",
                            "ra nhanh",
                            "len nhanh",
                            "khan cap",
                            "uu tien",
                            "urgent",
                            "asap",
                            "hurry"),
                    List.of("gấp", "vội", "khẩn", "nhanh")),
            new RuleDefinition(
                    "KIDS_ELDERLY_PREGNANT",
                    List.of(
                            "tre em",
                            "em be",
                            "be nho",
                            "mang thai",
                            "ba bau",
                            "nguoi gia",
                            "nguoi lon tuoi",
                            "toddler",
                            "kid",
                            "child",
                            "pregnant",
                            "elderly"),
                    List.of()));

    private static final List<RuleDefinition> DIETARY_RULES = List.of(
            new RuleDefinition("NO_SPICY", List.of("khong cay", "it cay", "no spicy", "mild"), List.of()),
            new RuleDefinition("VEGETARIAN", List.of("an chay", "vegetarian", "vegan", "thuan chay"), List.of()),
            new RuleDefinition(
                    "LESS_SEASONING",
                    List.of(
                            "it duong",
                            "khong duong",
                            "it da",
                            "khong da",
                            "khong hanh",
                            "khong ngo",
                            "khong rau",
                            "no onion",
                            "less sugar",
                            "no ice"),
                    List.of()),
            new RuleDefinition(
                    "NO_PORK_BEEF",
                    List.of("khong thit heo", "khong thit lon", "khong thit bo", "halal", "no pork", "no beef"),
                    List.of()));

    private NoteSafetyAnalyzer() {}

    public static NoteSafetyResult analyze(String rawNote) {
        if (rawNote == null || rawNote.isBlank()) {
            return new NoteSafetyResult(RISK_NORMAL, List.of(), List.of(), List.of());
        }

        String rawLowerPadded = padUnicodeWords(rawNote);
        String foldedPadded = padAsciiWords(foldAccents(rawNote));

        Set<String> allergens = matchRules(foldedPadded, rawLowerPadded, ALLERGEN_RULES);
        Set<String> urgency = matchRules(foldedPadded, rawLowerPadded, URGENCY_RULES);
        Set<String> dietary = matchRules(foldedPadded, rawLowerPadded, DIETARY_RULES);

        String riskLevel;
        if (!allergens.isEmpty()) {
            riskLevel = RISK_CRITICAL;
        } else if (!urgency.isEmpty()) {
            riskLevel = RISK_HIGH;
        } else {
            riskLevel = RISK_NORMAL;
        }

        return new NoteSafetyResult(
                riskLevel, new ArrayList<>(allergens), new ArrayList<>(urgency), new ArrayList<>(dietary));
    }

    public static int rankRisk(String riskLevel) {
        if (RISK_CRITICAL.equalsIgnoreCase(riskLevel)) {
            return 3;
        }
        if (RISK_HIGH.equalsIgnoreCase(riskLevel)) {
            return 2;
        }
        return 1;
    }

    public static String maxRisk(String first, String second) {
        return rankRisk(first) >= rankRisk(second) ? normalizeRisk(first) : normalizeRisk(second);
    }

    public static String normalizeRisk(String raw) {
        if (raw == null) {
            return RISK_NORMAL;
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if (RISK_CRITICAL.equals(upper) || RISK_HIGH.equals(upper) || RISK_NORMAL.equals(upper)) {
            return upper;
        }
        return RISK_NORMAL;
    }

    public static String foldAccents(String input) {
        String replacedD = input.replace('đ', 'd').replace('Đ', 'D');
        String decomposed = Normalizer.normalize(replacedD, Normalizer.Form.NFD);
        String stripped = COMBINING_MARKS.matcher(decomposed).replaceAll("");
        return MULTI_SPACE.matcher(stripped.toLowerCase(Locale.ROOT).trim()).replaceAll(" ");
    }

    private static Set<String> matchRules(String foldedPadded, String rawLowerPadded, List<RuleDefinition> rules) {
        Set<String> matched = new LinkedHashSet<>();
        for (RuleDefinition rule : rules) {
            boolean hit = rule.foldedKeywords().stream().anyMatch(kw -> foldedPadded.contains(" " + kw + " "))
                    || rule.rawKeywords().stream().anyMatch(kw -> rawLowerPadded.contains(" " + kw + " "));
            if (hit) {
                matched.add(rule.code());
            }
        }
        return matched;
    }

    private static String padAsciiWords(String normalizedNote) {
        String cleaned = normalizedNote.replaceAll("[^a-z0-9 ]", " ");
        return " " + MULTI_SPACE.matcher(cleaned.trim()).replaceAll(" ") + " ";
    }

    private static String padUnicodeWords(String rawNote) {
        String lowered = Normalizer.normalize(rawNote, Normalizer.Form.NFC).toLowerCase(VI_LOCALE);
        String cleaned = UNICODE_NON_WORD.matcher(lowered).replaceAll(" ");
        return " " + MULTI_SPACE.matcher(cleaned.trim()).replaceAll(" ") + " ";
    }

    public record NoteSafetyResult(
            String riskLevel, List<String> allergens, List<String> urgencyFlags, List<String> dietaryFlags) {}
}
