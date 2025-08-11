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

    @GetMapping("/{id}")
    public ResponseEntity<DiningTableResponse> getTableById(@PathVariable Long id) {
        return tableService.getTableById(id)
                .map(this::convertToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<DiningTableResponse>> getAllTables() {
        List<DiningTableResponse> responses = tableService.getAllTablesSorted()
                .stream().map(this::convertToResponse).collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<?> createTable(@RequestBody DiningTableRequest request) {
        try {
            DiningTable table = convertToEntity(request);
            DiningTable saved = tableService.createTable(table);
            return ResponseEntity.ok(convertToResponse(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // 400 + message
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTable(@PathVariable Long id, @RequestBody DiningTableRequest request) {
        try {
            DiningTable updated = tableService.updateStatusAndCapacity(id, request.getStatus(), request.getCapacity());
            return ResponseEntity.ok(convertToResponse(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // 400 + message
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTable(@PathVariable Long id) {
        try {
            tableService.deleteTable(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // 400 + message
        }
    }

     // === Helper methods ===
    private DiningTable convertToEntity(DiningTableRequest request) {
        DiningTable table = new DiningTable();
        
        table.setQrCodeUrl(request.getQrCodeUrl());
        table.setTableNumber(request.getTableNumber());
        table.setStatus(request.getStatus());
        table.setCapacity(request.getCapacity());
        return table;
    }

    private DiningTableResponse convertToResponse(DiningTable table) {
        DiningTableResponse response = new DiningTableResponse();
        response.setId(table.getId());
      
        response.setQrCodeUrl(table.getQrCodeUrl());
        response.setTableNumber(table.getTableNumber());
        response.setStatus(table.getStatus());
        response.setCapacity(table.getCapacity());
        return response;
    }
}
