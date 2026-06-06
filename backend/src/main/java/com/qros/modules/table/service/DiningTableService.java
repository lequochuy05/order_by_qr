package com.qros.modules.table.service;

import com.qros.infrastructure.storage.ImageManagerService;
import com.qros.modules.notification.service.NotificationService;
import com.qros.shared.transaction.TransactionSideEffectService;
import com.qros.modules.table.dto.DiningTableRequest;
import com.qros.modules.table.dto.DiningTableResponse;
import com.qros.modules.menu.dto.PublicMenuResponse;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.repository.DiningTableRepository;
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
 * DiningTableService - Manages dining table lifecycle, availability status, and
 * automated QR code generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiningTableService {

    private final DiningTableRepository repo;
    private final QRCodeService qrCodeService;
    private final ImageManagerService imageManagerService;
    private final NotificationService notificationService;
    private final TransactionSideEffectService sideEffects;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    /**
     * Retrieves all dining tables sorted by their display number.
     * 
     * @return List of dining table responses
     */
    @Cacheable(value = "tables", key = "'all_sorted'")
    public List<DiningTableResponse> getAllTablesSorted() {
        return repo.findAllByOrderByTableNumberAsc().stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Retrieves a single table entity by its identifier.
     * 
     * @param id Table ID
     * @return DiningTable entity
     * @throws ResponseStatusException if table is not found
     */
    public DiningTable getById(@NonNull Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    }

    /**
     * Retrieves table details in response DTO format.
     * 
     * @param id Table ID
     * @return DiningTableResponse
     */
    public DiningTableResponse getByIdResponse(@NonNull Long id) {
        return convertToResponse(getById(id));
    }

    /**
     * Locates a table using the unique code embedded in its QR.
     * 
     * @param tableCode Unique QR code
     * @return DiningTableResponse
     */
    @Cacheable(value = "tables", key = "'code_' + #tableCode")
    public DiningTableResponse getByTableCode(@NonNull String tableCode) {
        return repo.findByTableCode(tableCode)
                .map(this::convertToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid table code"));
    }

    @Cacheable(value = "tables", key = "'public_code_' + #tableCode")
    public PublicMenuResponse.Table getPublicByTableCode(@NonNull String tableCode) {
        return repo.findByTableCode(tableCode)
                .map(table -> new PublicMenuResponse.Table(table.getId(), table.getTableNumber()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid table code"));
    }

    /**
     * Retrieves a table by its display number (e.g., "A1").
     * 
     * @param tableNumber Display number
     * @return DiningTableResponse
     */
    public DiningTableResponse getByTableNumber(@NonNull String tableNumber) {
        return repo.findByTableNumber(tableNumber)
                .map(this::convertToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Table number not found: " + tableNumber));
    }

    /**
     * Filters tables based on their current availability or payment status.
     * 
     * @param status Target status
     * @return List of matching tables
     */
    public List<DiningTableResponse> getTablesByStatus(@NonNull DiningTable.TableStatus status) {
        return repo.findByStatus(status).stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Registers a new dining table and automatically generates its unique QR code
     * and Cloudinary storage link.
     * 
     * @param req Table creation request
     * @return Created table details
     */
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public DiningTableResponse create(DiningTableRequest req) {
        if (repo.existsByTableNumber(req.getTableNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Table number already exists");
        }

        String tableCode = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Map<String, String> qrMedia = generateQRMedia(tableCode);
        sideEffects.afterRollback(() -> imageManagerService.delete(qrMedia.get("publicId")),
                "delete rolled back table QR media " + tableCode);

        DiningTable table = DiningTable.builder()
                .tableNumber(req.getTableNumber())
                .capacity(req.getCapacity())
                .status(req.getStatus() != null ? req.getStatus() : DiningTable.TableStatus.AVAILABLE)
                .tableCode(tableCode)
                .qrCodeUrl(qrMedia.get("url"))
                .qrCodePublicId(qrMedia.get("publicId"))
                .build();

        DiningTable savedTable = repo.save(Objects.requireNonNull(table));
        notificationService.notifyTableChange();
        return convertToResponse(savedTable);
    }

    /**
     * Internal helper to generate a QR code pointing to the ordering URL and upload
     * it to cloud storage.
     */
    private Map<String, String> generateQRMedia(String tableCode) {
        try {
            String qrContent = frontendBaseUrl + "/menu?tableCode=" + tableCode;
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

    /**
     * Updates table configuration such as capacity or display number.
     * 
     * @param id  Table ID
     * @param req Update request
     * @return Updated table details
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

    /**
     * Regenerates the QR code for a specific table.
     * Useful when the base URL or path changes.
     *
     * @param id Table ID
     * @return Updated table details with new QR code
     */
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public DiningTableResponse regenerateQrCode(@NonNull Long id) {
        DiningTable table = getById(id);

        if (table.getQrCodePublicId() != null && !table.getQrCodePublicId().equals("PENDING")) {
            String oldPublicId = table.getQrCodePublicId();
            sideEffects.afterCommit(() -> {
                try {
                    imageManagerService.delete(oldPublicId);
                } catch (Exception e) {
                    log.error("Failed to delete old QR code", e);
                }
            }, "delete old table QR media " + id);
        }

        Map<String, String> qrMedia = generateQRMedia(table.getTableCode());
        table.setQrCodeUrl(qrMedia.get("url"));
        table.setQrCodePublicId(qrMedia.get("publicId"));

        DiningTable savedTable = repo.save(table);
        notificationService.notifyTableChange();
        return convertToResponse(savedTable);
    }

    /**
     * Soft deletes a table and cleans up its associated QR code from cloud storage.
     * 
     * @param id Table ID
     */
    @Transactional
    @CacheEvict(value = "tables", allEntries = true)
    public void delete(@NonNull Long id) {
        DiningTable table = getById(id);

        if (table.getQrCodePublicId() != null && !table.getQrCodePublicId().equals("PENDING")) {
            String publicId = table.getQrCodePublicId();
            sideEffects.afterCommit(() -> imageManagerService.delete(publicId),
                    "delete table QR media after table delete " + id);
        }

        repo.delete(Objects.requireNonNull(table));
        notificationService.notifyTableChange();
    }

    /**
     * Mapping helper to convert entity to DTO.
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
