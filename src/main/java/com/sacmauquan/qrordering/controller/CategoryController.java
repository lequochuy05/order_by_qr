package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.model.Category;
import com.sacmauquan.qrordering.repository.CategoryRepository;
import com.sacmauquan.qrordering.service.CategoryService;
import com.sacmauquan.qrordering.service.ImageManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;
    private final CategoryRepository repo;
    private final ImageManagerService imageManager;

    @GetMapping
    public List<Category> findAll() {
        return service.search(null, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
    }

    @GetMapping("/search")
    public Page<Category> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "name,asc") String sort
    ) {
        String[] s = sort.split(",");
        Sort sortObj = Sort.by(Sort.Direction.fromString(s.length > 1 ? s[1] : "asc"), s[0]);
        return service.search(q, PageRequest.of(page, size, sortObj));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Category dto) {
        try {
            return ResponseEntity.ok(service.create(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody Category dto) {
        try {
            return ResponseEntity.ok(service.update(id, dto));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        try {
            var cat = repo.findById(id).orElseThrow(() -> new NoSuchElementException("Không tìm thấy danh mục"));
            // 🧹 Xóa ảnh khỏi Cloudinary
            imageManager.delete(cat.getImg());
            repo.delete(cat);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<?> uploadImage(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        try {
            var cat = repo.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy danh mục"));
            if (file.isEmpty()) return ResponseEntity.badRequest().body("File rỗng");

            // 🧩 Thay ảnh (tự động xóa ảnh cũ nếu có)
            String newUrl = imageManager.replace(file, cat.getImg(), "order_by_qr/categories");
            cat.setImg(newUrl);
            repo.save(cat);

            return ResponseEntity.ok(Map.of("img", newUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
