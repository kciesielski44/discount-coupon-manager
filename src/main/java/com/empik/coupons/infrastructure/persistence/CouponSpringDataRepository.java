package com.empik.coupons.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Package-private interfejs JPA (szczegół implementacyjny adaptera).
 * Implementację tworzy Spring Data. Dostarcza to adapterowi mechanizm, który "gada" z bazą.
 * Dzięki temu domena widzi wyłącznie port {@link com.empik.coupons.domain.CouponRepository}.
 */
interface CouponSpringDataRepository extends JpaRepository<CouponEntity, UUID> {

    Optional<CouponEntity> findByCode(String code);

    boolean existsByCode(String code);
}
