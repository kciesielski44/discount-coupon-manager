package com.empik.coupons.infrastructure.persistence;

import com.empik.coupons.domain.Coupon;
import com.empik.coupons.domain.CouponCode;
import com.empik.coupons.domain.CouponRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Implementacja dla interfejsu adaptera portu {@link CouponRepository} oparta na JPA.
 * Tłumaczy domenę na encję w obie strony przez {@link CouponEntityMapper}.
 */
@Repository
class CouponRepositoryAdapter implements CouponRepository {

    private final CouponSpringDataRepository couponSpringDataRepository;

    CouponRepositoryAdapter(CouponSpringDataRepository couponSpringDataRepository) {
        this.couponSpringDataRepository = couponSpringDataRepository;
    }

    @Override
    public Coupon save(Coupon coupon) {
        CouponEntity saved = couponSpringDataRepository.save(CouponEntityMapper.toEntity(coupon));
        return CouponEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Coupon> findByCode(CouponCode code) {
        return couponSpringDataRepository.findByCode(code.value()).map(CouponEntityMapper::toDomain);
    }

    @Override
    public boolean existsByCode(CouponCode code) {
        return couponSpringDataRepository.existsByCode(code.value());
    }
}
