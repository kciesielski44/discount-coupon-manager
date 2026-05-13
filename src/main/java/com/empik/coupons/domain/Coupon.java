package com.empik.coupons.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregat kuponu rabatowego
 */
public record Coupon(
        UUID id,
        CouponCode code,
        Instant createdAt,
        int maxUsages,
        int currentUsages,
        CountryCode country
) {

    public Coupon {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(country, "country must not be null");
        if (maxUsages <= 0) {
            throw new IllegalArgumentException("maxUsages must be > 0. maxUsages: " + maxUsages);
        }
        if (currentUsages < 0) {
            throw new IllegalArgumentException("currentUsages must be >= 0. currentUsages: " + currentUsages);
        }
        if (currentUsages > maxUsages) {
            throw new IllegalArgumentException(
                    "currentUsages (%d) must not exceed maxUsages (%d)".formatted(currentUsages, maxUsages));
        }
    }

    /**
     * Rejestruje pojedyncze użycie i zwraca nowy stan kuponu. Oryginał pozostaje niezmieniony
     */
    public Coupon use() {
        if (currentUsages == maxUsages) {
            throw new CouponExhaustedException(code);
        }
        return new Coupon(id, code, createdAt, maxUsages, currentUsages + 1, country);
    }
}
