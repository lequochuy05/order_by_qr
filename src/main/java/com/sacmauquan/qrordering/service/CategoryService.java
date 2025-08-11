package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.model.Category;
import com.sacmauquan.qrordering.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CategoryService {
  private final CategoryRepository repo;

  public Page<Category> search(String q, Pageable pageable) {
    return repo.search(q, pageable);
  }

  public Category create(Category c) {
    if (repo.existsByNameIgnoreCase(c.getName()))
      throw new IllegalArgumentException("Tên danh mục đã tồn tại");
    return repo.save(c);
  }

  public Category update(Integer id, Category input) {
    Category exist = repo.findById(id)
      .orElseThrow(() -> new NoSuchElementException("Không tìm thấy danh mục"));

    if (!exist.getName().equalsIgnoreCase(input.getName())
        && repo.existsByNameIgnoreCase(input.getName())) {
      throw new IllegalArgumentException("Tên danh mục đã tồn tại");
    }
    exist.setName(input.getName());
    exist.setImg(input.getImg());
    return repo.save(exist);
  }

  public void delete(Integer id) {
    if (!repo.existsById(id)) throw new NoSuchElementException("Không tìm thấy danh mục");
    repo.deleteById(id);
  }
}
