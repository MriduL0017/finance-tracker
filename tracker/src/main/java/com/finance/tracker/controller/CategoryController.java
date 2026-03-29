package com.finance.tracker.controller;

import com.finance.tracker.model.Category;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @GetMapping
    public List<Category> getCategories() {
        return Arrays.asList(
            new Category(1, "Rent", "Expense"),
            new Category(2, "Mutual Fund SIP", "Investment"),
            new Category(3, "Life Insurance", "Expense")
        );
    }
    
    @GetMapping("/{id}")
    public Category getCategoryById(@PathVariable int id) {
        return new Category(id, "Mock Category", "Unknown");
    }
}