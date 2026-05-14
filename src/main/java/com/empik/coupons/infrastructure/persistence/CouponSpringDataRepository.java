package com.empik.coupons.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Atomowy inkrement licznika użyć kuponu — pojedynczy UPDATE z warunkiem chroni przed
     * race condition (Postgres serializuje konkurentów po row locku, każdy widzi nowy stan poprzednika).
     *
     * Zwraca: 1 = sukces, 0 = kupon nie istnieje albo osiągnął limit.
     *
     * clearAutomatically = true jest niezbędne, bo jeśli wcześniej w transakcji ktoś załadował tę encję
     * (np. UseCouponService.findByCode dla sprawdzenia kraju), to kontext trzyma jej stary snapshot
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE coupons
               SET current_usages = current_usages + 1
             WHERE code = :code
               AND current_usages < max_usages
            """, nativeQuery = true)
    int incrementUsagesIfAvailable(@Param("code") String code);
}
