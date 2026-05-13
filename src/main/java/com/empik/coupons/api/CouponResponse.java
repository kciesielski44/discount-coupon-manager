package com.empik.coupons.api;

import com.empik.coupons.domain.Coupon;

import java.time.Instant;
import java.util.UUID;

/**
 * Reprezentacja kuponu zwracana klientowi
 */
record CouponResponse(
        UUID id,
        String code,
        Instant createdAt,
        int maxUsages,
        int currentUsages,
        String country
) {

    static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.id(),
                coupon.code().value(),
                coupon.createdAt(),
                coupon.maxUsages(),
                coupon.currentUsages(),
                coupon.country().value()
        );
    }
}
