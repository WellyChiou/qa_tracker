package com.example.helloworld.controller.personal;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.personal.Expense;
import com.example.helloworld.service.personal.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/personal/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    /**
     * 獲取當前登入用戶的 UID
     */
    private String getCurrentUserUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            return authentication.getName(); // 通常是用戶的 UID
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Expense>>> getExpenses(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String member,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String mainCategory,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Expense> expenses = expenseService.getExpenses(year, month, member, type, mainCategory, page, size);
        return ResponseEntity.ok(ApiResponse.ok(expenses));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Expense>>> getAllExpenses(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String member,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String mainCategory) {
        List<Expense> expenses = expenseService.getAllExpenses(year, month, member, type, mainCategory);
        return ResponseEntity.ok(ApiResponse.ok(expenses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Expense>> getExpenseById(@PathVariable Long id) {
        return expenseService.getExpenseById(id)
                .map(e -> ResponseEntity.ok(ApiResponse.ok(e)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("支出記錄不存在")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Expense>> createExpense(@RequestBody Expense expense) {
        Expense saved = expenseService.saveExpense(expense);
        return ResponseEntity.ok(ApiResponse.ok(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Expense>> updateExpense(@PathVariable Long id, @RequestBody Expense expense) {
        return expenseService.getExpenseById(id)
                .map(existing -> {
                    if (expense.getDate() != null) existing.setDate(expense.getDate());
                    if (expense.getMember() != null) existing.setMember(expense.getMember());
                    if (expense.getType() != null) existing.setType(expense.getType());
                    if (expense.getMainCategory() != null) existing.setMainCategory(expense.getMainCategory());
                    if (expense.getSubCategory() != null) existing.setSubCategory(expense.getSubCategory());
                    if (expense.getAmount() != null) existing.setAmount(expense.getAmount());
                    if (expense.getCurrency() != null) existing.setCurrency(expense.getCurrency());
                    if (expense.getExchangeRate() != null) existing.setExchangeRate(expense.getExchangeRate());
                    if (expense.getDescription() != null) existing.setDescription(expense.getDescription());
                    if (expense.getUpdatedByUid() != null) existing.setUpdatedByUid(expense.getUpdatedByUid());
                    return ResponseEntity.ok(ApiResponse.ok(expenseService.saveExpense(existing)));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("支出記錄不存在")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
