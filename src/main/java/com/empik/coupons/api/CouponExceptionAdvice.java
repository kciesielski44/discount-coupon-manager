package com.empik.coupons.api;

import com.empik.coupons.domain.CouponDomainException;
import com.empik.coupons.domain.CouponErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Mapowanie wyjątków na strukturalne odpowiedzi błędów
 */
@RestControllerAdvice
class CouponExceptionAdvice {

    /**
     * Polimorficzny handler dla wszystkich wyjątków domenowych kuponu
     */
    @ExceptionHandler(CouponDomainException.class)
    ResponseEntity<ApiError> handleDomain(CouponDomainException ex) {
        HttpStatus status = httpStatusFor(ex.errorCode());
        return ResponseEntity
                .status(status)
                .body(new ApiError(ex.errorCode().name(), ex.getMessage()));
    }

    /**
     * Strukturyzacja błędów Bean Validation. Klient dostaje listę pól w details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("VALIDATION_FAILED", "Request validation failed", fields));
    }

    /**
     * Błędy konstrukcji value objectów (np. nieprawidłowy format CouponCode, nieznany CountryCode).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("INVALID_REQUEST", ex.getMessage()));
    }

    private static HttpStatus httpStatusFor(CouponErrorCode code) {
        return switch (code) {
            case COUPON_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case COUPON_EXHAUSTED, COUPON_CODE_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case COUNTRY_NOT_ALLOWED -> HttpStatus.FORBIDDEN;
            case CLIENT_COUNTRY_UNRESOLVED -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }
}
