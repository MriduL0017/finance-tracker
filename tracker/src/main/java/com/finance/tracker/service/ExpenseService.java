package com.finance.tracker.service;

import com.finance.tracker.entity.Expense;
import com.finance.tracker.repository.ExpenseRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ExpenseService {
	
	@Value("${python.ai.url}") 
	private String pythonUrl;
	
    private final ExpenseRepository expenseRepository;
    private final AiService aiService;
	
    public ExpenseService(ExpenseRepository expenseRepository, AiService aiService) {
        this.expenseRepository = expenseRepository;
        this.aiService = aiService;
    }
	
public Expense addExpense(Expense expense) {
        
        // 1. Check if React gave up and sent "Other"
        if ("Other".equals(expense.getCategory()) || expense.getCategory() == null) {
            try {
                // 2. Build the bridge to Python
                RestTemplate restTemplate = new RestTemplate();
                pythonUrl = pythonUrl+"/categorize";
                
                // 3. Package the description
                Map<String, String> request = new HashMap<>();
                request.put("description", expense.getDescription());
                
                // 4. Ask Python for the smart category!
                Map<String, String> response = restTemplate.postForObject(pythonUrl, request, Map.class);
                
                // 5. If Python gives us a better answer, overwrite the "Other" category
                if (response != null && response.containsKey("category")) {
                    expense.setCategory(response.get("category"));
                    System.out.println("Java intercepted 'Other' and AI corrected it to: " + response.get("category"));
                }
            } catch (Exception e) {
                // If the Python server is turned off, just quietly save it as "Other" so the app doesn't crash
                System.out.println("Could not reach Python AI. Defaulting to 'Other'.");
            }
        }
        
        // 6. Finally, save the expense to PostgreSQL
        return expenseRepository.save(expense);
    }
    
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }
    
    public Optional<Expense> getExpenseById(Integer id) {
        return expenseRepository.findById(id);
    }
    
    public void deleteExpense(Integer id) {
        expenseRepository.deleteById(id);
    }
    
	public Expense updateExpense(Integer id, Expense updatedExpense) {
		return expenseRepository.findById(id).map(expense -> {
			expense.setDescription(updatedExpense.getDescription());
			expense.setAmount(updatedExpense.getAmount());
			expense.setCategory(updatedExpense.getCategory());
			return expenseRepository.save(expense);
		}).orElseThrow(() -> new RuntimeException("Expense not found with id " + id));
	}
	
	public List<Expense> getExpensesByUserId(Integer userId) {
        return expenseRepository.findByUserId(userId);
    }
}