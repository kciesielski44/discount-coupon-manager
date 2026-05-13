package com.empik.coupons.domain;

/**
 * Bazowa klasa wyjątków domenowych kuponu. Każdy konkretny wyjątek deklaruje swoją kategorię przez errorCode
 */
public abstract class CouponDomainException extends RuntimeException {

    private final CouponCode code;

    protected CouponDomainException(String message, CouponCode code) {
        super(message);
        this.code = code;
    }

    public CouponCode code() {
        return code;
    }

    public abstract CouponErrorCode errorCode();
}
