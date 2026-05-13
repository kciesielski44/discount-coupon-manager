package com.empik.coupons.application;

import com.empik.coupons.domain.Coupon;
import com.empik.coupons.domain.CouponCodeAlreadyExistsException;

/**
 * Port wejściowy: utworzenie nowego kuponu.
 */
public interface CreateCouponUseCase {

    Coupon create(CreateCouponCommand command);
}
