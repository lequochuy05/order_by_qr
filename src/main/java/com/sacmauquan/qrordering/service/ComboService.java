package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.ComboItemRequest;
import com.sacmauquan.qrordering.dto.ComboRequest;
import com.sacmauquan.qrordering.model.*;
import com.sacmauquan.qrordering.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComboService {

    private final ComboRepository comboRepo;
    private final ComboItemRepository comboItemRepo;
    private final MenuItemRepository menuItemRepo;

    public List<Combo> getAll() {
        return comboRepo.findAll();
    }

    public Combo getById(Long id) {
        return comboRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy combo"));
    }

    // ================= CREATE =================
    @Transactional
    public Combo create(ComboRequest req) {
        if (comboRepo.existsByNameIgnoreCase(req.getName()))
            throw new RuntimeException("Tên combo đã tồn tại");

        Combo combo = new Combo();
        combo.setName(req.getName());
        combo.setPrice(req.getPrice());
        combo.setActive(req.getActive() != null ? req.getActive() : true);

        Combo saved = comboRepo.save(combo);

        if (req.getItems() != null) {
            for (ComboItemRequest itemReq : req.getItems()) {
                MenuItem menuItem = menuItemRepo.findById(itemReq.getMenuItemId())
                        .orElseThrow(() -> new RuntimeException("Món không tồn tại"));
                ComboItem ci = new ComboItem();
                ci.setCombo(saved);
                ci.setMenuItem(menuItem);
                ci.setQuantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : 1);
                comboItemRepo.save(ci);
            }
        }

        return saved;
    }

    // ================= UPDATE =================
    @Transactional
    public Combo update(Long id, ComboRequest req) {
        Combo combo = getById(id);
        combo.setName(req.getName());
        combo.setPrice(req.getPrice());
        combo.setActive(req.getActive() != null ? req.getActive() : combo.getActive());

        comboItemRepo.deleteByComboId(combo.getId());

        if (req.getItems() != null) {
            for (ComboItemRequest itemReq : req.getItems()) {
                MenuItem menuItem = menuItemRepo.findById(itemReq.getMenuItemId())
                        .orElseThrow(() -> new RuntimeException("Món không tồn tại"));
                ComboItem ci = new ComboItem();
                ci.setCombo(combo);
                ci.setMenuItem(menuItem);
                ci.setQuantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : 1);
                comboItemRepo.save(ci);
            }
        }

        return comboRepo.save(combo);
    }

    @Transactional
    public void delete(Long id) {
        comboItemRepo.deleteByComboId(id);
        comboRepo.deleteById(id);
    }

    @Transactional
    public Combo toggleActive(Long id) {
        Combo combo = getById(id);
        combo.setActive(!combo.getActive());
        return comboRepo.save(combo);
    }
}
