package com.empik.coupons.domain;

/**
 * Sygnalizuje konflikt unikalności kodu kuponu. Wyrzucany przez use case przed zapisem
 */
public final class CouponCodeAlreadyExistsException extends RuntimeException {

    private final CouponCode code;

    public CouponCodeAlreadyExistsException(CouponCode code) {
        super("Coupon with code '" + code.value() + "' already exists");
        this.code = code;
    }

    public CouponCode code() {
        return code;
    }
}
