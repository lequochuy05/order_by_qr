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

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(comboService.getById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ComboRequest req) {
        try {
            Combo combo = comboService.create(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(combo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ComboRequest req) {
        try {
            return ResponseEntity.ok(comboService.update(id, req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            comboService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<?> toggleActive(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(comboService.toggleActive(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
