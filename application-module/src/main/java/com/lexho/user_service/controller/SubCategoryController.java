package com.lexho.user_service.controller;

import com.lexho.catalog.entity.SubCategory;
import com.lexho.catalog.repository.SubCategoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subcategories")
public class SubCategoryController {

    private final SubCategoryRepository repo;

    public SubCategoryController(SubCategoryRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public SubCategory create(@RequestBody SubCategory subCategory) {
        return repo.save(subCategory);
    }

    @GetMapping("/category/{id}")
    public List<SubCategory> getByCategory(@PathVariable Long id) {
        return repo.findByCategoryId(id);
    }
}
