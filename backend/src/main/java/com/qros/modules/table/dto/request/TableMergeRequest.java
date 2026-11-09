package com.qros.modules.table.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TableMergeRequest(
        @NotNull Long primaryTableId,
        @NotNull @Size(min = 2, max = 6, message = "Select between 2 and 6 tables to merge") List<Long> tableIds,
        @NotNull @Min(1) Integer guestCount) {}
