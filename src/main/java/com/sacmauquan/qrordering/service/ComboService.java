package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.model.Combo;
import com.sacmauquan.qrordering.repository.ComboRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComboService {

    private final ComboRepository repo;

    public List<Combo> getAll() {
        return repo.findAll();
    }

    public Combo getById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy combo"));
    }
}
