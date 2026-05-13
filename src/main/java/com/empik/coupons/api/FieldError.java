package com.empik.coupons.api;

/**
 * Pojedynczy błąd walidacji pola DTO. Element listy ApiError.details dla błędów wymagających uszczegółowienia
 */
record FieldError(String field, String message) {
}
