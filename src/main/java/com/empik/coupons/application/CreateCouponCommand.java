package com.empik.coupons.application;

/**
 * Command tworzenia kuponu
 */
public record CreateCouponCommand(String code, int maxUsages, String country) {
}
