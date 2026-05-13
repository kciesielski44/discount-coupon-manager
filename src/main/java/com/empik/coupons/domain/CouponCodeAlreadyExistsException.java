package com.empik.coupons.domain;

/**
 * Sygnalizuje konflikt unikalności kodu kuponu. Wyrzucany przez use case przed zapisem
 */
public final class CouponCodeAlreadyExistsException extends CouponDomainException {

    public CouponCodeAlreadyExistsException(CouponCode code) {
        super("Coupon with code '" + code.value() + "' already exists", code);
    }

    @Override
    public CouponErrorCode errorCode() {
        return CouponErrorCode.COUPON_CODE_ALREADY_EXISTS;
    }
}
