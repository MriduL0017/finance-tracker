package com.finance.tracker.repository;

import com.finance.tracker.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExpenseRepository extends JpaRepository<Expense, Integer> {
    
	List<Expense> findByUserId(Integer userId);
	
	Page<Expense> findByUserIdOrderByDateDesc(Integer userId, Pageable pageable);
    
}