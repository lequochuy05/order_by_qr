package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.model.Combo;
import com.sacmauquan.qrordering.service.ComboService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/combos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    @GetMapping
    public List<Combo> getAllCombos() {
        return comboService.getAll();
    }

    @GetMapping("/{id}")
    public Combo getCombo(@PathVariable Long id) {
        return comboService.getById(id);
    }
}
