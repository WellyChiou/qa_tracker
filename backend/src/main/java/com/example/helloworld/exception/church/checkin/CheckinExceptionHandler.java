package com.example.helloworld.exception.church.checkin;

import com.example.helloworld.dto.church.checkin.ApiError;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CheckinExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiError> biz(BizException e) {
        return ResponseEntity.badRequest().body(new ApiError(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> dup(DataIntegrityViolationException e) {
        return ResponseEntity.badRequest().body(new ApiError("ALREADY_CHECKED_IN", "Already checked in."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unknown(Exception e) {
        return ResponseEntity.internalServerError().body(new ApiError("INTERNAL_ERROR", e.getMessage()));
    }
}

