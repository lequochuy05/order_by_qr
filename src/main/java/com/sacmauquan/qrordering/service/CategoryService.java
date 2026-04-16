package com.sacmauquan.qrordering.service;

import org.springframework.lang.NonNull;

import com.sacmauquan.qrordering.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface CategoryService {
    List<Category> getAll();

    Page<Category> search(String q, @NonNull Pageable pageable);

    Category create(@NonNull Category c);

    Category update(@NonNull Integer id, @NonNull Category input);

    void delete(@NonNull Integer id);

    Map<String, Object> uploadImage(@NonNull Integer id, @NonNull MultipartFile file);
}
