package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.model.DiningTable;

import java.util.List;
import java.util.Optional;

public interface DiningTableService {
    List<DiningTable> getAllTablesSorted();
    Optional<DiningTable> getTableById(Long id);
    Optional<DiningTable> getTableByCode(String code);
    DiningTable createTable(DiningTable table);
    DiningTable updateStatusAndCapacity(Long id, String status, Integer capacity);
    void deleteTable(Long id);
}
