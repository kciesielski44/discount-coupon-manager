package com.empik.coupons.domain;

/**
 * Sygnalizuje próbę użycia kuponu, który osiągnął już maksymalną liczbę użyć
 */
public final class CouponExhaustedException extends CouponDomainException {

    public CouponExhaustedException(CouponCode code) {
        super("Coupon with code '" + code.value() + "' has reached its maximum number of usages", code);
    }

    @Override
    public CouponErrorCode errorCode() {
        return CouponErrorCode.COUPON_EXHAUSTED;
    }
}
