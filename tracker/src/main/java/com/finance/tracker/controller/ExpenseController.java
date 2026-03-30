package com.finance.tracker.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.finance.tracker.entity.Expense;
import com.finance.tracker.repository.ExpenseRepository;
import com.finance.tracker.service.ExpenseService;
import com.finance.tracker.service.AiService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
    
    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;
    private final AiService aiService;

    // 2. Inject it in the constructor
    public ExpenseController(ExpenseService expenseService, ExpenseRepository expenseRepository, AiService aiService) {
        this.expenseService = expenseService;
        this.expenseRepository = expenseRepository;
        this.aiService = aiService;
    }
    
    @PostMapping
    public Expense addExpense(@Valid @RequestBody Expense expense) {
        return expenseService.addExpense(expense);
    }
    
    @GetMapping
    public List<Expense> getAllExpenses() {
        return expenseService.getAllExpenses();
    }
    
    @GetMapping("/{id}")
    public Optional<Expense> getExpenseById(@PathVariable Integer id) {
        return expenseService.getExpenseById(id);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable Integer id) {
        return expenseRepository.findById(id).map(expense -> {
            expenseRepository.delete(expense);
            return ResponseEntity.ok("Expense deleted successfully");
        }).orElseThrow(() -> new RuntimeException("Expense not found"));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateExpense(@PathVariable Integer id, @RequestBody Expense expenseDetails) {
        return expenseRepository.findById(id).map(expense -> {
            expense.setDescription(expenseDetails.getDescription());
            // Added amount update just in case you need it!
            expense.setAmount(expenseDetails.getAmount()); 
            expense.setCategory(expenseDetails.getCategory());
            expenseRepository.save(expense);
            return ResponseEntity.ok("Expense updated successfully");
        }).orElseThrow(() -> new RuntimeException("Expense not found"));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<Expense>> getUserExpenses(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Expense> expensePage = expenseRepository.findByUserIdOrderByDateDesc(userId, pageable);
        return ResponseEntity.ok(expensePage);
    }
    
    // --- THE NEW, CLEAN RECEIPT SCANNER ---
    @PostMapping(value = "/upload-receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadReceipt(@RequestParam("file") MultipartFile file) {
        try {
            // 3. Just hand the file to our dedicated AiService!
            Map<String, Object> aiResponse = aiService.scanReceipt(file);
            
            // If the AI service returned our fallback error, pass a 500 status to React
            if (aiResponse.containsKey("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(aiResponse);
            }
            
            return ResponseEntity.ok(aiResponse);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to scan receipt.");
        }
    }
}