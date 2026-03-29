package com.finance.tracker.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.finance.tracker.entity.Expense;
import com.finance.tracker.repository.ExpenseRepository;
import com.finance.tracker.service.ExpenseService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
	
	@Value("${python.ai.url}") 
	private String pythonUrl;
    
	private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository; // 2. DECLARE THE REPO

    // 3. UPDATE THE CONSTRUCTOR TO REQUIRE BOTH
    public ExpenseController(ExpenseService expenseService, ExpenseRepository expenseRepository) {
        this.expenseService = expenseService;
        this.expenseRepository = expenseRepository;
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
        
        // Create a "Page Request" chunking 20 items at a time
        Pageable pageable = PageRequest.of(page, size);
        
        // Fetch and return the specific page
        Page<Expense> expensePage = expenseRepository.findByUserIdOrderByDateDesc(userId, pageable);
        return ResponseEntity.ok(expensePage);
    }
    
 // --- THE NEW RECEIPT SCANNER BRIDGE ---
    @PostMapping(value = "/upload-receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadReceipt(@RequestParam("file") MultipartFile file) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // This is the magic wrapper that tricks Python into accepting the forwarded file!
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename(); // Crucial!
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            pythonUrl = pythonUrl+"/read-receipt";
            
            // Ask Python to read the receipt!
            ResponseEntity<Map> response = restTemplate.postForEntity(pythonUrl, requestEntity, Map.class);
            return ResponseEntity.ok(response.getBody());
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to scan receipt.");
        }
    }
}