package com.sacmauquan.qrordering.service.impl;

import com.sacmauquan.qrordering.model.DiningTable;
import com.sacmauquan.qrordering.repository.DiningTableRepository;
import com.sacmauquan.qrordering.service.DiningTableService;
import com.sacmauquan.qrordering.service.ImageManagerService;
import com.sacmauquan.qrordering.service.NotificationService;
import com.sacmauquan.qrordering.service.QRCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DiningTableServiceImpl implements DiningTableService {

    private final DiningTableRepository repo;
    private final QRCodeService qrCodeService;
    private final ImageManagerService imageManagerService;
    private final NotificationService notificationService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public List<DiningTable> getAllTablesSorted() {
        List<DiningTable> tables = repo.findAll();
        tables.sort(Comparator.comparingInt(t -> {
            try {
                return Integer.parseInt(t.getTableNumber());
            } catch (NumberFormatException e) {
                return 0;
            }
        }));
        return tables;
    }

    @Override
    public Optional<DiningTable> getTableById(Long id) {
        return repo.findById(id);
    }

    @Override
    public Optional<DiningTable> getTableByCode(String code) {
        return repo.findByTableCode(code);
    }

    @Override
    public DiningTable createTable(DiningTable table) {
        if (repo.existsByTableNumber(table.getTableNumber()))
            throw new IllegalArgumentException("Số bàn đã tồn tại");

        if (table.getCapacity() <= 0)
            throw new IllegalArgumentException("Sức chứa phải > 0");

        table.setStatus((table.getStatus() == null || table.getStatus().isBlank()) ? DiningTable.AVAILABLE : table.getStatus());

        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        table.setTableCode(code);

        DiningTable saved = repo.save(table);

        try {
            String qrContent = frontendBaseUrl + "/menu?tableCode=" + saved.getTableCode();
            byte[] qrBytes = qrCodeService.generateQRCodeImage(qrContent, 300, 300);

            String folder = "order_by_qr/tables";
            String publicId = "qr_" + saved.getTableCode();
            Map<String, Object> result = imageManagerService.uploadBytes(qrBytes, folder, publicId);

            saved.setQrCodeUrl(result.get("secure_url").toString());
            saved.setQrCodePublicId(result.get("public_id").toString());

            repo.save(saved);

            notificationService.notifyTableChange();
            return saved;
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo mã QR: " + e.getMessage(), e);
        }
    }

    @Override
    public DiningTable updateStatusAndCapacity(Long id, String status, Integer capacity) {
        DiningTable t = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bàn không tồn tại với id " + id));

        if (capacity == null || capacity <= 0)
            throw new IllegalArgumentException("Sức chứa phải lớn hơn 0");

        if (status != null && !status.isBlank())
            t.setStatus(status);

        t.setCapacity(capacity);
        DiningTable saved = repo.save(t);

        notificationService.notifyTableChange();
        return saved;
    }

    @Override
    public void deleteTable(Long id) {
        DiningTable t = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bàn không tồn tại"));

        if (t.getQrCodeUrl() != null)
            imageManagerService.delete(t.getQrCodeUrl());

        repo.delete(t);
        notificationService.notifyTableChange();
    }
}
