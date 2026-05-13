package com.empik.coupons.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Wspólny format błędu API
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ApiError(String code, String message, List<FieldError> details) {

    ApiError(String code, String message) {
        this(code, message, null);
    }
}
