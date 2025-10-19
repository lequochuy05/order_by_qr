package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.DiningTableRequest;
import com.sacmauquan.qrordering.dto.DiningTableResponse;
import com.sacmauquan.qrordering.model.DiningTable;
import com.sacmauquan.qrordering.service.DiningTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "*")
public class DiningTableController {

    @Autowired
    private DiningTableService tableService;

    @GetMapping
    public ResponseEntity<List<DiningTableResponse>> getAllTables() {
        List<DiningTableResponse> responses = tableService.getAllTablesSorted()
                .stream().map(this::convertToResponse).collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiningTableResponse> getTableById(@PathVariable Long id) {
        return tableService.getTableById(id)
                .map(this::convertToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //  Truy vấn bàn theo mã QR (dành cho khách khi quét mã)
    @GetMapping("/code/{tableCode}")
    public ResponseEntity<DiningTableResponse> getTableByCode(@PathVariable String tableCode) {
        return tableService.getTableByCode(tableCode)
                .map(this::convertToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createTable(@RequestBody DiningTableRequest request) {
        try {
            DiningTable table = convertToEntity(request);
            DiningTable saved = tableService.createTable(table);
            return ResponseEntity.ok(convertToResponse(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTable(@PathVariable Long id, @RequestBody DiningTableRequest request) {
        try {
            DiningTable updated = tableService.updateStatusAndCapacity(id, request.getStatus(), request.getCapacity());
            return ResponseEntity.ok(convertToResponse(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTable(@PathVariable Long id) {
        try {
            tableService.deleteTable(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ===== Helper methods =====
    private DiningTable convertToEntity(DiningTableRequest request) {
        DiningTable table = new DiningTable();
        table.setQrCodeUrl(request.getQrCodeUrl());
        table.setTableNumber(request.getTableNumber());
        table.setStatus(request.getStatus());
        table.setCapacity(request.getCapacity());
        return table;
    }

    private DiningTableResponse convertToResponse(DiningTable table) {
        DiningTableResponse res = new DiningTableResponse();
        res.setId(table.getId());
        res.setQrCodeUrl(table.getQrCodeUrl());
        res.setTableNumber(table.getTableNumber());
        res.setStatus(table.getStatus());
        res.setCapacity(table.getCapacity());
        return res;
    }
}
