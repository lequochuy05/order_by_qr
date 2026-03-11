package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.model.DiningTable;
import com.sacmauquan.qrordering.repository.DiningTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DiningTableService {

    private final DiningTableRepository repo;
    private final QRCodeService qrCodeService;
    private final ImageManagerService imageManagerService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

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

   public Optional<DiningTable> getTableById(Long id) {
        return repo.findById(id);
    }

    public Optional<DiningTable> getTableByCode(String code) {
        return repo.findByTableCode(code);
    }

    public DiningTable createTable(DiningTable table) {
        if (repo.existsByTableNumber(table.getTableNumber()))
            throw new IllegalArgumentException("Số bàn đã tồn tại");

        if (table.getCapacity() <= 0)
            throw new IllegalArgumentException("Sức chứa phải > 0");

        table.setStatus((table.getStatus() == null || table.getStatus().isBlank()) ? "Trống" : table.getStatus());

        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        table.setTableCode(code);

        DiningTable saved = repo.save(table);

        try {
            String qrContent = frontendBaseUrl + "/menu.html?tableCode=" + saved.getTableCode();
            byte[] qrBytes = qrCodeService.generateQRCodeImage(qrContent, 300, 300);

            String folder = "order_by_qr/tables";
            String publicId = "qr_" + saved.getTableCode();
            Map result = imageManagerService.uploadBytes(qrBytes, folder, publicId);

            saved.setQrCodeUrl(result.get("secure_url").toString());
            saved.setQrCodePublicId(result.get("public_id").toString());
            
            repo.save(saved);
            
            notifyChange(); // 3. Báo hiệu thay đổi
            return saved;
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo mã QR: " + e.getMessage(), e);
        }
    }

    public DiningTable updateStatusAndCapacity(Long id, String status, Integer capacity) {
        DiningTable t = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bàn không tồn tại với id " + id));

        if (capacity == null || capacity <= 0)
            throw new IllegalArgumentException("Sức chứa phải lớn hơn 0");

        if (status != null && !status.isBlank())
            t.setStatus(status);

        t.setCapacity(capacity);
        DiningTable saved = repo.save(t);
        
        notifyChange(); // 3. Báo hiệu thay đổi
        return saved;
    }

    public void deleteTable(Long id) {
        DiningTable t = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bàn không tồn tại"));

        if (t.getQrCodeUrl() != null)
            imageManagerService.delete(t.getQrCodeUrl());

        repo.delete(t);
        notifyChange(); // 3. Báo hiệu thay đổi
    }

    // 4. Hàm gửi tín hiệu chuẩn
    private void notifyChange() {
        eventPublisher.publishEvent(new com.sacmauquan.qrordering.event.WebSocketEvent(
                "/topic/tables", 
                "UPDATED", 
                "⚡ [WS] DiningTable change -> Sent UPDATED signal"
        ));
    }
}
