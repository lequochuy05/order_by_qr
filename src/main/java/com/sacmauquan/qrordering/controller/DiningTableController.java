package com.sacmauquan.qrordering.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sacmauquan.qrordering.model.DiningTable;
import com.sacmauquan.qrordering.repository.DiningTableRepository;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "*") // Cho phép gọi từ frontend
public class DiningTableController {

    @Autowired
    private DiningTableRepository diningTableRepository;

    @GetMapping("/{id}")
    public ResponseEntity<DiningTable> getTableById(@PathVariable Long id) {
        return diningTableRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}