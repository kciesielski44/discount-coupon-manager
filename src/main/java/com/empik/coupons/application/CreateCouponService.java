package com.empik.coupons.application;

import com.empik.coupons.domain.CountryCode;
import com.empik.coupons.domain.Coupon;
import com.empik.coupons.domain.CouponCode;
import com.empik.coupons.domain.CouponCodeAlreadyExistsException;
import com.empik.coupons.domain.CouponRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Realizacja portu CreateCouponUseCase.
 *
 * Konflikt kodu kuponmu jest sprawdzany jawnie (existsByCode) z dwóch powodów:
 * daje czytelny wyjątek domenowy zamiast wycieku
 * i pozwala kontrolerowi mapować błąd bez znajomości szczegółów infrastruktury.
 */
@Service
class CreateCouponService implements CreateCouponUseCase {

    private final CouponRepository repository;

    CreateCouponService(CouponRepository repository) {
        this.repository = repository;
    }

    /**
     * TODO  transakcja ? teoretyczna utrata atomowości na create ???
     * Bez transakcji teoretycznie inne żądanie mogłoby wcisnąć duplikat między tymi wywołaniami (existsByCode, save)
     * Alee z Transactional przy poziomie izolacji read_commited (default przy postgresie) również istnieje chyba takie ryzyko ??
     * bo existsByCode nie zakłada locka na nieistniejący wiersz. Ostateczny strażnik unikalności to i tak UNIQUE constraint w bazie
     * Implementacja bez Transakcji teoretycznie powinna wystarczyć na potrzeby tworzenia kuponu
     * Do weryfikacji na koniec
     */
    @Override
    public Coupon create(CreateCouponCommand command) {
        CouponCode code = new CouponCode(command.code());
        if (repository.existsByCode(code)) {
            throw new CouponCodeAlreadyExistsException(code);
        }
        Coupon coupon = new Coupon(
                UUID.randomUUID(),
                code,
                Instant.now(),
                command.maxUsages(),
                0,
                new CountryCode(command.country())
        );
        return repository.save(coupon);
    }
}
