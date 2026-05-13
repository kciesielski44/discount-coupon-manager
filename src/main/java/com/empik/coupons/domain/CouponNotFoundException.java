package com.empik.coupons.domain;

/**
 * Sygnalizuje próbę operacji na kuponie o nieznanym/nieistniejącym kodzie
 */
public final class CouponNotFoundException extends RuntimeException {

    private final CouponCode code;

    public CouponNotFoundException(CouponCode code) {
        super("Coupon with code '" + code.value() + "' not found");
        this.code = code;
    }

    public CouponCode code() {
        return code;
    }
}
