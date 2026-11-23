package com.qros.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.qros.modules.ai.dto.response.AiInvoiceOcrResponse;
import com.qros.modules.ai.dto.response.AiInvoiceOcrResponse.ExtractedInvoiceLine;
import com.qros.modules.ai.gateway.AiChatGateway;
import com.qros.modules.ai.support.AiPromptBuilder;
import com.qros.modules.inventory.model.InventoryItem;
import com.qros.modules.inventory.repository.InventoryItemRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.validation.ImageFileValidator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

class AiInvoiceOcrServiceTest {

    private static final byte[] VALID_JPEG_HEADER =
            new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};

    private InventoryItemRepository inventoryItemRepository;
    private AiChatGateway aiChatGateway;
    private AiInvoiceOcrService service;

    @BeforeEach
    void setUp() {
        inventoryItemRepository = mock(InventoryItemRepository.class);
        aiChatGateway = mock(AiChatGateway.class);

        service = new AiInvoiceOcrService(
                new ImageFileValidator(),
                inventoryItemRepository,
                aiChatGateway,
                new AiPromptBuilder(),
                new ObjectMapper(),
                new SimpleMeterRegistry());
    }

    @Test
    void rejectsInvalidImageFileBeforeCallingGateway() {
        MockMultipartFile spoofed = new MockMultipartFile("file", "bad.jpg", "image/jpeg", "NOT_A_JPEG".getBytes());

        assertThatThrownBy(() -> service.parseInvoice(spoofed, "vi"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FILE_INVALID);

        verifyNoInteractions(inventoryItemRepository, aiChatGateway);
    }

    @Test
    void parsesVisionJsonConvertsUnitsFuzzyMatchesAndFlagsUnmatchedOrMismatch() {
        InventoryItem beef = InventoryItem.builder()
                .id(1L)
                .name("Thịt bò Úc")
                .unit("g")
                .unitCost(new BigDecimal("200.00"))
                .active(true)
                .build();
        InventoryItem milk = InventoryItem.builder()
                .id(2L)
                .name("Sữa tươi nguyên kem")
                .unit("ml")
                .unitCost(new BigDecimal("35.00"))
                .active(true)
                .build();
        InventoryItem tomato = InventoryItem.builder()
                .id(3L)
                .name("Cà chua bi")
                .unit("hộp")
                .unitCost(new BigDecimal("25000.00"))
                .active(true)
                .build();

        when(inventoryItemRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(beef, milk, tomato));
        when(aiChatGateway.chatWithImage(anyString(), anyString(), any(byte[].class), anyString()))
                .thenReturn("""
                ```json
                {
                  "supplierName": "Thực phẩm An Phú",
                  "invoiceDate": "2026-11-22",
                  "invoiceNumber": "HD-2026-88",
                  "totalAmount": 755000,
                  "warnings": ["Một dòng viết tay hơi mờ"],
                  "lines": [
                    {
                      "rawName": "Thịt bò Úc nhập khẩu",
                      "quantity": 2.5,
                      "unit": "kg",
                      "unitPrice": 220000,
                      "lineTotal": 550000,
                      "matchedInventoryItemId": 1,
                      "confidence": "HIGH"
                    },
                    {
                      "rawName": "Sua tuoi nguyen kem",
                      "quantity": 3,
                      "unit": "lít",
                      "lineTotal": 105000,
                      "matchedInventoryItemId": null,
                      "confidence": "MEDIUM"
                    },
                    {
                      "rawName": "Cà chua bi",
                      "quantity": 2,
                      "unit": "kg",
                      "unitPrice": 30000,
                      "matchedInventoryItemId": 3,
                      "confidence": "HIGH"
                    },
                    {
                      "rawName": "Saffron Tây Ban Nha",
                      "quantity": 1,
                      "unit": "lọ",
                      "unitPrice": 40000,
                      "matchedInventoryItemId": 9999,
                      "confidence": "LOW"
                    }
                  ]
                }
                ```
                """);

        MockMultipartFile receipt = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", VALID_JPEG_HEADER);

        AiInvoiceOcrResponse response = service.parseInvoice(receipt, "vi");

        assertThat(response.aiGenerated()).isTrue();
        assertThat(response.supplierName()).isEqualTo("Thực phẩm An Phú");
        assertThat(response.invoiceNumber()).isEqualTo("HD-2026-88");
        assertThat(response.warnings()).containsExactly("Một dòng viết tay hơi mờ");
        assertThat(response.lines()).hasSize(4);

        // Line 1: 2.5 kg -> 2500.000 g; 220,000đ/kg -> 220.00đ/g
        ExtractedInvoiceLine line1 = response.lines().get(0);
        assertThat(line1.matchStatus()).isEqualTo("MATCHED");
        assertThat(line1.unitConverted()).isTrue();
        assertThat(line1.unitMismatch()).isFalse();
        assertThat(line1.convertedQuantity()).isEqualByComparingTo("2500.000");
        assertThat(line1.convertedUnitCost()).isEqualByComparingTo("220.00");

        // Line 2: matched via fuzzy Vietnamese fallback when ID was null; l -> ml + derived price
        ExtractedInvoiceLine line2 = response.lines().get(1);
        assertThat(line2.matchedInventoryItemId()).isEqualTo(2L);
        assertThat(line2.matchStatus()).isEqualTo("MATCHED");
        assertThat(line2.unitConverted()).isTrue();
        assertThat(line2.convertedQuantity()).isEqualByComparingTo("3000.000");
        assertThat(line2.convertedUnitCost()).isEqualByComparingTo("35.00");

        // Line 3: kg invoice vs hộp stock -> UNIT_MISMATCH warning
        ExtractedInvoiceLine line3 = response.lines().get(2);
        assertThat(line3.matchStatus()).isEqualTo("UNIT_MISMATCH");
        assertThat(line3.unitMismatch()).isTrue();
        assertThat(line3.conversionNote()).contains("Đơn vị hóa đơn");

        // Line 4: hallucinated ID 9999 stripped -> UNMATCHED
        ExtractedInvoiceLine line4 = response.lines().get(3);
        assertThat(line4.matchedInventoryItemId()).isNull();
        assertThat(line4.matchStatus()).isEqualTo("UNMATCHED");
    }

    @Test
    void returnsBilingualFallbackWhenVisionGatewayIsDisabled() {
        when(inventoryItemRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());
        when(aiChatGateway.chatWithImage(anyString(), anyString(), any(byte[].class), anyString()))
                .thenReturn("Tính năng trợ lý AI hiện chưa được bật.");

        MockMultipartFile receipt = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", VALID_JPEG_HEADER);

        AiInvoiceOcrResponse response = service.parseInvoice(receipt, "en");

        assertThat(response.aiGenerated()).isFalse();
        assertThat(response.language()).isEqualTo("en");
        assertThat(response.lines()).isEmpty();
        assertThat(response.warnings()).hasSize(1);
        assertThat(response.warnings().get(0)).contains("AI Vision invoice scanner");
    }
}
