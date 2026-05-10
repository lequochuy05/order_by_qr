package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.DiningTableRequest;
import com.sacmauquan.qrordering.dto.DiningTableResponse;
import com.sacmauquan.qrordering.model.DiningTable;
import com.sacmauquan.qrordering.repository.DiningTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * DiningTableService - Manages dining tables and QR Codes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiningTableService {

    private final DiningTableRepository repo;
    private final QRCodeService qrCodeService;
    private final ImageManagerService imageManagerService;
    private final NotificationService notificationService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    /*
     * Get All Tables
     */
    @Cacheable(value = "tables", key = "'all_sorted'")
    public List<DiningTableResponse> getAllTablesSorted() {
        return repo.findAllByOrderByTableNumberAsc().stream()
                .map(this::convertToResponse)
                .toList();
    }

    /*
     * Get Table by Id
     */
    public DiningTable getById(@NonNull Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    }

    /*
     * Get Table by Id Response
     */
    public DiningTableResponse getByIdResponse(@NonNull Long id) {
        return convertToResponse(getById(id));
    }

    /*
     * Get Table by Table Code
     */
    public DiningTableResponse getByTableCode(@NonNull String tableCode) {
        return repo.findByTableCode(tableCode)
                .map(this::convertToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid table code"));
    }

    /*
     * Get Table by Table Number
     */
    public DiningTableResponse getByTableNumber(@NonNull String tableNumber) {
        return repo.findByTableNumber(tableNumber)
                .map(this::convertToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Table number not found: " + tableNumber));
    }

    /*
     * Get Table by Status
     */
    public List<DiningTableResponse> getTablesByStatus(@NonNull DiningTable.TableStatus status) {
        return repo.findByStatus(status).stream()
                .map(this::convertToResponse)
                .toList();
    }

    /*
     * Create Table
     */
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public DiningTableResponse create(DiningTableRequest req) {
        if (repo.existsByTableNumber(req.getTableNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Table number already exists");
        }
        // Create tableCode
        String tableCode = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        // Create and Upload QR Code
        Map<String, String> qrMedia = generateQRMedia(tableCode);
        // Initialize table object
        DiningTable table = DiningTable.builder()
                .tableNumber(req.getTableNumber())
                .capacity(req.getCapacity())
                .status(req.getStatus() != null ? req.getStatus() : DiningTable.TableStatus.AVAILABLE)
                .tableCode(tableCode)
                .qrCodeUrl(qrMedia.get("url"))
                .qrCodePublicId(qrMedia.get("publicId"))
                .build();
        // Save table
        DiningTable savedTable = repo.save(Objects.requireNonNull(table));
        notificationService.notifyTableChange();
        return convertToResponse(savedTable);
    }

    /*
     * Generate QR Code
     */
    private Map<String, String> generateQRMedia(String tableCode) {
        try {
            String qrContent = frontendBaseUrl + "/order?tableCode=" + tableCode;
            byte[] qrBytes = qrCodeService.generateQRCodeImage(qrContent, 300, 300);
            String folder = "order_by_qr/tables";
            String publicId = "qr_" + tableCode;
            Map<String, Object> result = imageManagerService.uploadBytes(qrBytes, folder, publicId);
            return Map.of(
                    "url", result.get("secure_url").toString(),
                    "publicId", result.get("public_id").toString());
        } catch (Exception e) {
            log.error("Error generating QR code: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "System error generating QR code");
        }
    }

    /*
     * Update Table
     */
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public DiningTableResponse update(@NonNull Long id, DiningTableRequest req) {
        DiningTable table = getById(id);

        if (repo.existsByTableNumberAndIdNot(req.getTableNumber(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Table number already exists");
        }

        table.setTableNumber(req.getTableNumber());
        table.setCapacity(req.getCapacity());
        table.setStatus(req.getStatus() != null ? req.getStatus() : table.getStatus());

        DiningTable saved = repo.save(table);
        notificationService.notifyTableChange();
        return convertToResponse(saved);
    }

    /*
     * Delete Table
     */
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public void delete(@NonNull Long id) {
        DiningTable table = getById(id);

        // Delete QR code image on Cloudinary
        if (table.getQrCodePublicId() != null && !table.getQrCodePublicId().equals("PENDING")) {
            try {
                imageManagerService.delete(table.getQrCodePublicId());
            } catch (Exception e) {
                log.error("Error deleting QR code image ID {}: {}", id, e.getMessage());
            }
        }

        repo.delete(Objects.requireNonNull(table));
        notificationService.notifyTableChange();
    }

    /*
     * Convert Table to Response
     */
    private DiningTableResponse convertToResponse(DiningTable table) {
        return new DiningTableResponse(
                table.getId(),
                table.getTableNumber(),
                table.getTableCode(),
                table.getStatus().name(),
                table.getCapacity(),
                table.getQrCodeUrl(),
                table.getCreatedAt(),
                table.getUpdatedAt());
    }
}
