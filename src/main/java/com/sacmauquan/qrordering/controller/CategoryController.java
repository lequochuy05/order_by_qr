package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.model.Category;
import com.sacmauquan.qrordering.service.CategoryService;
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

    @GetMapping
    public List<Category> findAll() {
        return service.search(null, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
    }

    @GetMapping("/search")
    public Page<Category> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        String[] sortParams = Objects.requireNonNullElse(sort, "id,asc").split(",");
        Sort.Direction direction = sortParams.length > 1 ? Sort.Direction.fromString(sortParams[1])
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, Objects.requireNonNull(sortParams[0])));
        return service.search(q, Objects.requireNonNull(pageable));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Category dto) {
        return ResponseEntity.ok(service.create(Objects.requireNonNull(dto)));
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable Integer id, @Valid @RequestBody Category category) {
        return service.update(Objects.requireNonNull(id), Objects.requireNonNull(category));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(Objects.requireNonNull(id));
    }

    @PostMapping("/{id}/image")
    public Map<String, Object> uploadImage(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        return service.uploadImage(Objects.requireNonNull(id),
                Objects.requireNonNull(file));
    }

}
