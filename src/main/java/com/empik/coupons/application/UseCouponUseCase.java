package com.empik.coupons.application;

import com.empik.coupons.domain.Coupon;

/**
 * Port wejściowy: rejestracja użycia istniejącego kuponu.
 */
public interface UseCouponUseCase {

    Coupon use(UseCouponCommand command);
}
