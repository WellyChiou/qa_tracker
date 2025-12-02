package com.example.helloworld.service;

import com.example.helloworld.entity.Expense;
import com.example.helloworld.entity.User;
import com.example.helloworld.repository.ExpenseRepository;
import com.example.helloworld.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    public Page<Expense> getExpenses(Integer year, Integer month, String member, String type, 
                                     String mainCategory, int page, int size) {
        // 默認不過濾用戶，讓所有用戶都能看到所有記錄
        String currentUserUid = getCurrentUserUid();
        Pageable pageable = PageRequest.of(page, size);
        return expenseRepository.findByFilters(currentUserUid, false, year, month, member, type, mainCategory, pageable);
    }

    public List<Expense> getAllExpenses(Integer year, Integer month, String member, String type, String mainCategory) {
        // 默認不過濾用戶，讓所有用戶都能看到所有記錄
        String currentUserUid = getCurrentUserUid();
        return expenseRepository.findByFiltersList(currentUserUid, false, year, month, member, type, mainCategory);
    }

    public List<Expense> getExpensesByUserUid(String userUid) {
        return expenseRepository.findByCreatedByUid(userUid);
    }

    public Optional<Expense> getExpenseById(Long id) {
        return expenseRepository.findById(id);
    }

    public Expense saveExpense(Expense expense) {
        // 設置用戶 ID
        String currentUserUid = getCurrentUserUid();
        if (expense.getCreatedByUid() == null) {
            expense.setCreatedByUid(currentUserUid);
        }
        expense.setUpdatedByUid(currentUserUid);

        return expenseRepository.save(expense);
    }

    /**
     * 獲取當前登入用戶的 UID
     */
    private String getCurrentUserUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName();
            // 根據 username 查找對應的 uid
            return userRepository.findByUsername(username)
                    .map(User::getUid)
                    .orElse(null);
        }
        return null;
    }

    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
    }

    public List<Expense> getExpensesByDateRange(LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByDateBetween(startDate, endDate);
    }
}

