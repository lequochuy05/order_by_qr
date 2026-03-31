package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.ComboRequest;
import com.sacmauquan.qrordering.model.Combo;

import java.util.List;

public interface ComboService {
    List<Combo> getAll();
    List<Combo> getAllActive();
    Combo getById(Long id);
    Combo create(ComboRequest req);
    Combo update(Long id, ComboRequest req);
    void delete(Long id);
    Combo toggleActive(Long id);
}
