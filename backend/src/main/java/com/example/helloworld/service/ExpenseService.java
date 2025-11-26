package com.example.helloworld.service;

import com.example.helloworld.entity.Expense;
import com.example.helloworld.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    public Page<Expense> getExpenses(Integer year, Integer month, String member, String type, 
                                     String mainCategory, int page, int size) {
        // 排序已在 Repository 查詢中指定（按日期降序，再按建立時間降序）
        Pageable pageable = PageRequest.of(page, size);
        return expenseRepository.findByFilters(year, month, member, type, mainCategory, pageable);
    }

    public List<Expense> getAllExpenses(Integer year, Integer month, String member, String type, String mainCategory) {
        return expenseRepository.findByFiltersList(year, month, member, type, mainCategory);
    }

    public Optional<Expense> getExpenseById(Long id) {
        return expenseRepository.findById(id);
    }

    public Expense saveExpense(Expense expense) {
        return expenseRepository.save(expense);
    }

    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
    }

    public List<Expense> getExpensesByDateRange(LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByDateBetween(startDate, endDate);
    }
}

