package com.empik.coupons.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
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
     * Atomowy inkrement licznika użyć kuponu w jednym zapytaniu.
     *
     * Warunek "AND currentUsages < maxUsages" sprawia że Postgres serializuje konkurujące updaty po
     * row locku, każdy widzi już zaktualizowany stan poprzednika. RETURNING zwraca nowy
     * stan bez dodatkowego selecta po sukcesie.
     *
     * Empty Optional = zero zmienionych wierszy (kupon nie istnieje albo osiągnął limit).
     * Adnotacja @Modifying nie ma tutaj zastosowania. Spring Data odrzuca RETURNING idąc ścieżką executeUpdate
     */
    @Query(value = """
            UPDATE coupons
               SET current_usages = current_usages + 1
             WHERE code = :code
               AND current_usages < max_usages
            RETURNING *
            """, nativeQuery = true)
    Optional<CouponEntity> incrementUsagesIfAvailable(@Param("code") String code);
}
