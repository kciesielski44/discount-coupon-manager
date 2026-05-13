package com.empik.coupons.api;

import com.empik.coupons.domain.CouponCodeAlreadyExistsException;
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
}
