package com.empik.coupons.application;

/**
 * Command rejestracji użycia kuponu
 */
public record UseCouponCommand(String code, String clientIp) {
}
