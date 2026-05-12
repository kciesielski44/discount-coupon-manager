package com.empik.coupons.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final CouponCode CODE = new CouponCode("promo");
    private static final CountryCode COUNTRY = new CountryCode("PL");
    private static final Instant CREATED_AT = Instant.parse("2026-05-13T10:15:30Z");

    @Test
    void constructsValidCoupon() {
        Coupon coupon = new Coupon(ID, CODE, CREATED_AT, 10, 3, COUNTRY);

        assertThat(coupon.id()).isEqualTo(ID);
        assertThat(coupon.code()).isEqualTo(CODE);
        assertThat(coupon.createdAt()).isEqualTo(CREATED_AT);
        assertThat(coupon.maxUsages()).isEqualTo(10);
        assertThat(coupon.currentUsages()).isEqualTo(3);
        assertThat(coupon.country()).isEqualTo(COUNTRY);
    }

    @Test
    void allowsNewCreatedCouponWithZeroUsages() {
        assertThat(new Coupon(ID, CODE, CREATED_AT, 10, 0, COUNTRY).currentUsages())
                .isZero();
    }

    @Test
    void allowsFullyExhaustedCoupon() {
        assertThat(new Coupon(ID, CODE, CREATED_AT, 10, 10, COUNTRY).currentUsages())
                .isEqualTo(10);
    }

    @Test
    void rejectsNullId() {
        assertThatThrownBy(() -> new Coupon(null, CODE, CREATED_AT, 10, 0, COUNTRY))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullCode() {
        assertThatThrownBy(() -> new Coupon(ID, null, CREATED_AT, 10, 0, COUNTRY))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullCreatedAt() {
        assertThatThrownBy(() -> new Coupon(ID, CODE, null, 10, 0, COUNTRY))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullCountry() {
        assertThatThrownBy(() -> new Coupon(ID, CODE, CREATED_AT, 10, 0, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsZeroMaxUsages() {
        assertThatThrownBy(() -> new Coupon(ID, CODE, CREATED_AT, 0, 0, COUNTRY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxUsages");
    }

    @Test
    void rejectsNegativeMaxUsages() {
        assertThatThrownBy(() -> new Coupon(ID, CODE, CREATED_AT, -1, 0, COUNTRY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxUsages");
    }

    @Test
    void rejectsNegativeCurrentUsages() {
        assertThatThrownBy(() -> new Coupon(ID, CODE, CREATED_AT, 10, -1, COUNTRY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currentUsages");
    }

    @Test
    void rejectsCurrentUsagesAboveMax() {
        assertThatThrownBy(() -> new Coupon(ID, CODE, CREATED_AT, 10, 11, COUNTRY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currentUsages");
    }
}
