package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.model.Category;
import com.sacmauquan.qrordering.repository.CategoryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*") // Cho phép fetch từ file HTML
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping("/api/categories")
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
}
