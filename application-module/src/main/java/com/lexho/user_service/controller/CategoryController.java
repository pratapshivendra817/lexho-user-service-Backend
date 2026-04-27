package com.lexho.user_service.controller;

import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.catalog.repository.CategoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    private final CategoryRepository repo;

    public CategoryController(CategoryRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public ServiceCategory create(@RequestBody ServiceCategory category) {
        return repo.save(category);
    }

    @GetMapping
    public List<ServiceCategory> getAll() {
        return repo.findAll();
    }
}

