package com.empik.coupons.application;

import com.empik.coupons.domain.Coupon;
import com.empik.coupons.domain.CouponCode;
import com.empik.coupons.domain.CouponNotFoundException;
import com.empik.coupons.domain.CouponRepository;
import org.springframework.stereotype.Service;

/**
 * Implementacja portu UseCouponUseCase
 */
@Service
class UseCouponService implements UseCouponUseCase {

    private final CouponRepository repository;

    UseCouponService(CouponRepository repository) {
        this.repository = repository;
    }

    @Override
    public Coupon use(UseCouponCommand command) {
        CouponCode code = new CouponCode(command.code());
        Coupon coupon = repository.findByCode(code)
                .orElseThrow(() -> new CouponNotFoundException(code));
        return repository.save(coupon.use());
    }
}
