package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.model.Category;
import com.sacmauquan.qrordering.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.Optional;

import java.nio.file.*;
import java.util.UUID;
import java.util.List;
import java.util.NoSuchElementException;

import com.sacmauquan.qrordering.repository.CategoryRepository;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
  private final CategoryService service;
  private final CategoryRepository repo;

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
      service.delete(id);
      return ResponseEntity.noContent().build();
    } catch (NoSuchElementException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
  }

  @PostMapping("/{id}/image")
  public ResponseEntity<?> uploadImage(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
      try {
          var cat = repo.findById(id).orElseThrow(() -> new NoSuchElementException("Không tìm thấy danh mục"));
          if (file.isEmpty()) return ResponseEntity.badRequest().body("File rỗng");

          // Thư mục public để máy khách truy cập: /uploads/categories/...
          Path publicDir = Paths.get("uploads/categories"); // ghi ra ./uploads/categories
          Files.createDirectories(publicDir);

          String ext = Optional.ofNullable(file.getOriginalFilename())
                  .filter(n -> n.contains("."))
                  .map(n -> n.substring(n.lastIndexOf(".")))
                  .orElse(".jpg");
          String filename = UUID.randomUUID().toString().replaceAll("-", "") + ext;

          Path dest = publicDir.resolve(filename);
          Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

          String publicPath = "/uploads/categories/" + filename;

          cat.setImg(publicPath);
          repo.save(cat);

          return ResponseEntity.ok(Map.of("img", publicPath));
      } catch (Exception e) {
          return ResponseEntity.badRequest().body(e.getMessage());
      }
  }
}
