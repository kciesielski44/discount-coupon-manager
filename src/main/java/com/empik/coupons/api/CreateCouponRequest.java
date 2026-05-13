package com.empik.coupons.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Żądanie utworzenia kuponu
 */
record CreateCouponRequest(

        @NotBlank
        String code,

        @NotNull
        @Min(1)
        Integer maxUsages,

        @NotBlank
        String country
) {
}
