package com.qros.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BackendMessageLanguageTest {

    private static final Pattern VIETNAMESE_CHARACTER = Pattern.compile("[À-ỹĐđ]");
    private static final Path MAIN_SOURCE = Path.of("src/main/java");

    @Test
    void technicalBackendMessagesRemainEnglish() throws Exception {
        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(MAIN_SOURCE)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !isLocalizedContent(path))
                    .forEach(path -> collectViolations(path, violations));
        }

        assertThat(violations)
                .as("Backend API, validation and business messages must remain English")
                .isEmpty();
    }

    private static boolean isLocalizedContent(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return normalized.contains("/modules/ai/") || normalized.contains("/infrastructure/mail/");
    }

    private static void collectViolations(Path path, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(path);
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                String trimmed = line.trim();

                if (trimmed.startsWith("//")
                        || trimmed.startsWith("/*")
                        || trimmed.startsWith("*")
                        || !VIETNAMESE_CHARACTER.matcher(line).find()) {
                    continue;
                }

                violations.add(path + ":" + (index + 1) + " -> " + trimmed);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to inspect backend messages in " + path, exception);
        }
    }
}
