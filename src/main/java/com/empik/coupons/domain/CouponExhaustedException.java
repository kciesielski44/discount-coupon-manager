package com.empik.coupons.domain;

/**
 * Sygnalizuje próbę użycia kuponu, który osiągnął już maksymalną liczbę użyć
 */
public final class CouponExhaustedException extends RuntimeException {

    private final CouponCode code;

    public CouponExhaustedException(CouponCode code) {
        super("Coupon with code '" + code.value() + "' has reached its maximum number of usages");
        this.code = code;
    }

    public CouponCode code() {
        return code;
    }
}
