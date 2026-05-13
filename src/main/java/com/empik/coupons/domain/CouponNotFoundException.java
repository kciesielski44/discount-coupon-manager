package com.empik.coupons.domain;

/**
 * Sygnalizuje próbę operacji na kuponie o nieznanym/nieistniejącym kodzie
 */
public final class CouponNotFoundException extends CouponDomainException {

    public CouponNotFoundException(CouponCode code) {
        super("Coupon with code '" + code.value() + "' not found", code);
    }

    @Override
    public CouponErrorCode errorCode() {
        return CouponErrorCode.COUPON_NOT_FOUND;
    }
}
