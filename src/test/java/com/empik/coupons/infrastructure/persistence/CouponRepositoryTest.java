package com.empik.coupons.infrastructure.persistence;

import com.empik.coupons.domain.CountryCode;
import com.empik.coupons.domain.Coupon;
import com.empik.coupons.domain.CouponCode;
import com.empik.coupons.domain.CouponRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/**
 * Test integracyjny adaptera na realnym Postgresie przez Testcontainers
 *
 * @DataJpaTest ładuje JPA - encje, repozytoria Spring Data, transakcyjny rollback
 * po każdym teście. Bez kontrolerów i warstwy aplikacji, więc kontekst startuje szybko
 *
 * Replace.NONE jako zabezpieczenie wyłącza domyślne podstawianie np. H2 z @DataJpaTest.
 * Nawet gdyby H2 była w classpath (nie dodane ceklowo), testy powinny działać
 * na tej samej bazie co "produkcja" (inaczej różnice w SQL dialect i typach
 * potrafią przepuścić błędy migracji aż na środowisko docelowe)
 *
 * @ServiceConnection wpina kontener jako DataSource bez ręcznego @DynamicPropertySource
 *
 * @Import dodany z uwagi, że DataJpaTest nie skanuje po @Repository
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Import(CouponRepositoryAdapter.class)
class CouponRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private CouponRepository repository;

    @Test
    void savesAndRetrievesCouponByCode() {
        Coupon saved = repository.save(newCoupon("PROMO_2026"));

        Optional<Coupon> found = repository.findByCode(new CouponCode("PROMO_2026"));

        assertThat(found).contains(saved);
    }

    @Test
    void lookupIsCaseInsensitive() {
        repository.save(newCoupon("Wiosna"));

        assertThat(repository.findByCode(new CouponCode("WIOSNA")))
                .isPresent();
        assertThat(repository.findByCode(new CouponCode("wiosna")))
                .isPresent();
        assertThat(repository.findByCode(new CouponCode("WiOsNa")))
                .isPresent();
    }

    @Test
    void returnsEmptyWhenCodeNotFound() {
        assertThat(repository.findByCode(new CouponCode("not_existing")))
                .isEmpty();
    }

    @Test
    void existsByCodeRespectsCaseInsensitiveContract() {
        repository.save(newCoupon("Spring2026"));

        assertThat(repository.existsByCode(new CouponCode("SPRING2026")))
                .isTrue();
        assertThat(repository.existsByCode(new CouponCode("missing")))
                .isFalse();
    }

    private static Coupon newCoupon(String code) {
        return new Coupon(
                UUID.randomUUID(),
                new CouponCode(code),
                Instant.now(),
                10,
                0,
                new CountryCode("PL"));
    }
}
