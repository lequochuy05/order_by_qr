package com.sacmauquan.qrordering.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import com.sacmauquan.qrordering.model.DiningTable;
import jakarta.validation.constraints.Min;

/**
 * DiningTableRequest - Data transfer object for creating or updating a dining table.
 */
@Data
public class DiningTableRequest {
    /**
     * Unique identifier number or label for the table.
     */
    @NotBlank(message = "Table number cannot be empty")
    private String tableNumber;

    /**
     * Current availability status of the table.
     */
    private DiningTable.TableStatus status;

    /**
     * Maximum number of people the table can accommodate.
     */
    @Min(value = 1, message = "Capacity must be at least 1 person")
    private int capacity;
}
