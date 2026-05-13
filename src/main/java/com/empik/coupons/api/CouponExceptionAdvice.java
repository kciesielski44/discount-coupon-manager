package com.empik.coupons.api;

import com.empik.coupons.domain.CouponCodeAlreadyExistsException;
import com.empik.coupons.domain.CouponExhaustedException;
import com.empik.coupons.domain.CouponNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Mapowanie wyjątków domenowych na odpowiedzi HTTP.
 */
@RestControllerAdvice
class CouponExceptionAdvice {

    /**
     * Konflikt duplikatu kodu (409)
     */
    @ExceptionHandler(CouponCodeAlreadyExistsException.class)
    ResponseEntity<ApiError> handleAlreadyExists(CouponCodeAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError("COUPON_CODE_ALREADY_EXISTS", ex.getMessage()));
    }

    /**
     * Nieznany kod kuponu (404)
     */
    @ExceptionHandler(CouponNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(CouponNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiError("COUPON_NOT_FOUND", ex.getMessage()));
    }

    /**
     * Kupon osiągnął już maksymalną liczbę użyć (409)
     */
    @ExceptionHandler(CouponExhaustedException.class)
    ResponseEntity<ApiError> handleExhausted(CouponExhaustedException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError("COUPON_EXHAUSTED", ex.getMessage()));
    }
}
