package com.sacmauquan.qrordering.service.impl;

import com.sacmauquan.qrordering.dto.ComboItemRequest;
import com.sacmauquan.qrordering.dto.ComboRequest;
import com.sacmauquan.qrordering.model.Combo;
import com.sacmauquan.qrordering.model.ComboItem;
import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.ComboItemRepository;
import com.sacmauquan.qrordering.repository.ComboRepository;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import com.sacmauquan.qrordering.service.ComboService;
import com.sacmauquan.qrordering.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComboServiceImpl implements ComboService {

    private final ComboRepository comboRepo;
    private final ComboItemRepository comboItemRepo;
    private final MenuItemRepository menuItemRepo;
    private final NotificationService notificationService;

    @Override
    public List<Combo> getAll() {
        return comboRepo.findAll();
    }

    @Override
    public List<Combo> getAllActive() {
        return comboRepo.findByActiveTrue();
    }

    @Override
    public Combo getById(Long id) {
        return comboRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy combo"));
    }

    @Override
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
        
        notificationService.notifyComboChange("create", saved.getId());
        return saved;
    }

    @Override
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
        
        Combo saved = comboRepo.save(combo);
        notificationService.notifyComboChange("update", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        comboItemRepo.deleteByComboId(id);
        comboRepo.deleteById(id);
        
        notificationService.notifyComboChange("delete", id);
    }

    @Override
    @Transactional
    public Combo toggleActive(Long id) {
        Combo combo = getById(id);
        
        boolean isActive = Boolean.TRUE.equals(combo.getActive());
        combo.setActive(!isActive);
        
        Combo saved = comboRepo.saveAndFlush(combo);
        
        notificationService.notifyComboChange("update", saved.getId());
        return saved;
    }
}
