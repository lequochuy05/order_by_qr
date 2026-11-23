package com.qros.modules.ai.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.qros.modules.ai.support.NoteSafetyAnalyzer.NoteSafetyResult;
import org.junit.jupiter.api.Test;

class NoteSafetyAnalyzerTest {

    @Test
    void detectsVietnameseAllergensWithAndWithoutDiacritics() {
        NoteSafetyResult accented = NoteSafetyAnalyzer.analyze("Khách bị dị ứng đậu phộng và hải sản");
        assertThat(accented.riskLevel()).isEqualTo(NoteSafetyAnalyzer.RISK_CRITICAL);
        assertThat(accented.allergens()).contains("ALLERGY", "PEANUT_NUTS", "SEAFOOD");

        NoteSafetyResult unaccented = NoteSafetyAnalyzer.analyze("khong bo bot ngot, di ung sua bo");
        assertThat(unaccented.riskLevel()).isEqualTo(NoteSafetyAnalyzer.RISK_CRITICAL);
        assertThat(unaccented.allergens()).contains("ALLERGY", "MSG", "DAIRY");
    }

    @Test
    void foldsVietnameseDAndDetectsHighUrgencyNotes() {
        NoteSafetyResult result = NoteSafetyAnalyzer.analyze("Đẩy gấp giúp bàn có em bé và bà bầu");

        assertThat(result.riskLevel()).isEqualTo(NoteSafetyAnalyzer.RISK_HIGH);
        assertThat(result.allergens()).isEmpty();
        assertThat(result.urgencyFlags()).contains("URGENT", "KIDS_ELDERLY_PREGNANT");
    }

    @Test
    void detectsDietaryPreferencesWithoutInflatingRisk() {
        NoteSafetyResult result = NoteSafetyAnalyzer.analyze("Làm không cay, ít đường giúp mình");

        assertThat(result.riskLevel()).isEqualTo(NoteSafetyAnalyzer.RISK_NORMAL);
        assertThat(result.dietaryFlags()).contains("NO_SPICY", "LESS_SEASONING");
    }

    @Test
    void avoidsFalsePositivesOnAccentedVietnameseHomographs() {
        NoteSafetyResult everydayNote =
                NoteSafetyAnalyzer.analyze("Bàn số 5 cần ghế trẻ em cho con của khách, sửa giúp lượng đá");

        assertThat(everydayNote.riskLevel()).isEqualTo(NoteSafetyAnalyzer.RISK_HIGH);
        assertThat(everydayNote.allergens()).isEmpty();
        assertThat(everydayNote.urgencyFlags()).containsExactly("KIDS_ELDERLY_PREGNANT");
    }

    @Test
    void maxRiskAndRankNeverDowngradeCritical() {
        assertThat(NoteSafetyAnalyzer.maxRisk(NoteSafetyAnalyzer.RISK_CRITICAL, NoteSafetyAnalyzer.RISK_NORMAL))
                .isEqualTo(NoteSafetyAnalyzer.RISK_CRITICAL);
        assertThat(NoteSafetyAnalyzer.maxRisk(NoteSafetyAnalyzer.RISK_NORMAL, NoteSafetyAnalyzer.RISK_HIGH))
                .isEqualTo(NoteSafetyAnalyzer.RISK_HIGH);
        assertThat(NoteSafetyAnalyzer.rankRisk(NoteSafetyAnalyzer.RISK_CRITICAL))
                .isGreaterThan(NoteSafetyAnalyzer.rankRisk(NoteSafetyAnalyzer.RISK_HIGH));
        assertThat(NoteSafetyAnalyzer.rankRisk(NoteSafetyAnalyzer.RISK_HIGH))
                .isGreaterThan(NoteSafetyAnalyzer.rankRisk(NoteSafetyAnalyzer.RISK_NORMAL));
    }
}
