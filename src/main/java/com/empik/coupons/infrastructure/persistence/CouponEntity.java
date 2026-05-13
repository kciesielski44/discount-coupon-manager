package com.empik.coupons.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Encja JPA odpowiadająca agregatowi {@link com.empik.coupons.domain.Coupon}.
 * Świadomie trzymana oddzielnie od domeny — domena pozostaje wolna od adnotacji persystencji,
 * a encja od reguł biznesowych.
 * Konwersja odbywa się przez {@link CouponEntityMapper}.
 */
@Entity
@Table(name = "coupons")
class CouponEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "max_usages", nullable = false)
    private int maxUsages;

    @Column(name = "current_usages", nullable = false)
    private int currentUsages;

    @Column(nullable = false, length = 2)
    private String country;

    // wymagane przez JPA
    protected CouponEntity() {}

    CouponEntity(UUID id, String code, Instant createdAt, int maxUsages, int currentUsages, String country) {
        this.id = id;
        this.code = code;
        this.createdAt = createdAt;
        this.maxUsages = maxUsages;
        this.currentUsages = currentUsages;
        this.country = country;
    }

    UUID getId() {
        return id;
    }

    String getCode() {
        return code;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    int getMaxUsages() {
        return maxUsages;
    }

    int getCurrentUsages() {
        return currentUsages;
    }

    String getCountry() {
        return country;
    }
}
