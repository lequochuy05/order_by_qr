package com.qros.modules.table.service;

import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TableCodeGenerator {

    private static final int MAX_ATTEMPTS = 10;
    private static final int TABLE_CODE_LENGTH = 20;

    private final DiningTableRepository tableRepo;

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String tableCode = UUID.randomUUID().toString().replace("-", "").substring(0, TABLE_CODE_LENGTH);

            if (!tableRepo.existsByTableCode(tableCode)) {
                return tableCode;
            }
        }

        throw new BusinessException(ErrorCode.TABLE_QR_GENERATION_FAILED, "Unable to generate unique table code");
    }
}
