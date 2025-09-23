package com.sacmauquan.qrordering.service;

import java.util.Map;

import com.sacmauquan.qrordering.model.Category;
import com.sacmauquan.qrordering.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CategoryService {
  private final CategoryRepository repo;
  private final SimpMessagingTemplate broker; 

  public Page<Category> search(String q, Pageable pageable) {
    return repo.search(q, pageable);
  }

  public Category create(Category c) {
    if (repo.existsByNameIgnoreCase(c.getName()))
      throw new IllegalArgumentException("Tên danh mục đã tồn tại");
    
    Category saved = repo.save(c);
    broker.convertAndSend("/topic/categories",     // 2) GỬI SAU
        Map.of("event", "changed", "id", saved.getId()));
    return saved;
  }

  public Category update(Integer id, Category input) {
    Category exist = repo.findById(id)
      .orElseThrow(() -> new NoSuchElementException("Không tìm thấy danh mục"));

    if (!exist.getName().equalsIgnoreCase(input.getName())
        && repo.existsByNameIgnoreCase(input.getName())) {
      throw new IllegalArgumentException("Tên danh mục đã tồn tại");
    }
    exist.setName(input.getName());
    if (input.getImg() != null && !input.getImg().trim().isEmpty()) {
        exist.setImg(input.getImg());
    }
    Category saved = repo.save(exist);             // LƯU TRƯỚC
    broker.convertAndSend("/topic/categories",
        Map.of("event","changed","id", saved.getId()));  // GỬI SAU
    return saved;
  }

  public void delete(Integer id) {
    if (!repo.existsById(id)) throw new NoSuchElementException("Không tìm thấy danh mục");
    repo.deleteById(id);                           // XOÁ TRƯỚC
    broker.convertAndSend("/topic/categories",
        Map.of("event","deleted","id", id));       // GỬI SAU
  }
}
