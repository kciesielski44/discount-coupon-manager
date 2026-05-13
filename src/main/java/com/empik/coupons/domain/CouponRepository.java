package com.empik.coupons.domain;

import java.util.Optional;

/**
 * Port wyjściowy persystencji w warstwie domenowej
 * Implementacja (com.empik.coupons.infrastructure.persistence.CouponRepositoryAdapter) w warstwie
 * infrastructure (domena nie wie nic o JPA)
 */
public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findByCode(CouponCode code);

    boolean existsByCode(CouponCode code);
}
