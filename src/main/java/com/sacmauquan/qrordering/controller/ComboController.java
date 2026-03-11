package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.ComboRequest;
import com.sacmauquan.qrordering.model.Combo;
import com.sacmauquan.qrordering.service.ComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/combos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ComboController {

    private final ComboService comboService;

    @GetMapping
    public List<Combo> getAll() {
        return comboService.getAll();
    }

    @GetMapping("/active")
    public ResponseEntity<List<Combo>> getAllActiveCombos() {
        return ResponseEntity.ok(comboService.getAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(comboService.getById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ComboRequest req) {
        Combo combo = comboService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(combo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ComboRequest req) {
        return ResponseEntity.ok(comboService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        comboService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<?> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(comboService.toggleActive(id));
    }
}
