package com.empik.coupons.application;

import com.empik.coupons.domain.Coupon;
import com.empik.coupons.domain.CouponCode;
import com.empik.coupons.domain.CouponExhaustedException;
import com.empik.coupons.domain.CouponNotFoundException;
import com.empik.coupons.domain.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacja portu UseCouponUseCase
 */
@Service
class UseCouponService implements UseCouponUseCase {

    private final CouponRepository repository;

    UseCouponService(CouponRepository repository) {
        this.repository = repository;
    }

    /**
     * Pojedynczy atomowy update w bazie ("WHERE currentUsages < maxUsages") gwarantuje, że
     * dowolna liczba równoległych żądań nie przekroczy limitu. Postgres serializuje konkurentów po row locku.
     * Istnienie sprawdzamy dopiero w ścieżce błędu, żeby rozróżnić 404 od 409. Spójny widok obu zapytań w jednej
     * transakcji wyklucza scenariusz "update zwrócił 0, ale w międzyczasie ktoś usunął kupon".
     */
    @Override
    @Transactional
    public Coupon use(UseCouponCommand command) {
        CouponCode code = new CouponCode(command.code());
        return repository.registerUsage(code)
                .orElseThrow(() -> resolveFailure(code));
    }

    private RuntimeException resolveFailure(CouponCode code) {
        if (repository.existsByCode(code)) {
            return new CouponExhaustedException(code);
        }
        return new CouponNotFoundException(code);
    }
}
