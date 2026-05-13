package com.empik.coupons.api;

/**
 * Wspólny format błędu API
 */
record ApiError(String code, String message) {
}
