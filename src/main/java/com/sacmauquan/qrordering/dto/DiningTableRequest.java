package com.sacmauquan.qrordering.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

import com.sacmauquan.qrordering.model.DiningTable;

import jakarta.validation.constraints.Min;

@Data
public class DiningTableRequest {
    @NotBlank(message = "Số bàn không được để trống")
    private String tableNumber;

    private DiningTable.TableStatus status; // Mặc định là AVAILABLE

    @Min(value = 1, message = "Sức chứa phải ít nhất là 1 người")
    private int capacity;
}
