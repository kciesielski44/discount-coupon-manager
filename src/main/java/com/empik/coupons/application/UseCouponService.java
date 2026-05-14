package com.empik.coupons.application;

import com.empik.coupons.domain.ClientCountryUnresolvedException;
import com.empik.coupons.domain.CountryCode;
import com.empik.coupons.domain.Coupon;
import com.empik.coupons.domain.CouponCode;
import com.empik.coupons.domain.CouponCountryNotAllowedException;
import com.empik.coupons.domain.CouponExhaustedException;
import com.empik.coupons.domain.CouponNotFoundException;
import com.empik.coupons.domain.CouponRepository;
import com.empik.coupons.domain.GeoIp;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Implementacja portu UseCouponUseCase
 */
@Service
class UseCouponService implements UseCouponUseCase {

    private final CouponRepository repository;
    private final GeoIp geoIp;

    UseCouponService(CouponRepository repository, GeoIp geoIp) {
        this.repository = repository;
        this.geoIp = geoIp;
    }

    /**
     * Sprawdzanie kraju przed registerUsage (a nie po) eliminuje konieczność rollbacku zwiększonego
     * licznika, gdy klient jest z niedozwolonego kraju. Pojedyncza transakcja gwarantuje, że obie
     * operacje widzą spójny widok kuponu (country jest stałą agregatu, ale @Transactional
     * dokumentuje granicę i daje rollback na wypadek wyjątku w trakcie)
     */
    @Override
    @Transactional
    public Coupon use(UseCouponCommand command) {
        CouponCode code = new CouponCode(command.code());
        Coupon coupon = repository.findByCode(code)
                .orElseThrow(() -> new CouponNotFoundException(code));

        CountryCode clientCountry = resolveClientCountry(coupon, command.clientIp());
        if (!clientCountry.equals(coupon.country())) {
            throw new CouponCountryNotAllowedException(code, clientCountry, coupon.country());
        }

        return repository.registerUsage(code)
                .orElseThrow(() -> new CouponExhaustedException(code));
    }

    private CountryCode resolveClientCountry(Coupon coupon, String clientIp) {
        if (isUnroutable(clientIp)) {
            throw new ClientCountryUnresolvedException(coupon.code(), clientIp);
        }
        return geoIp.lookup(clientIp)
                .orElseThrow(() -> new ClientCountryUnresolvedException(coupon.code(), clientIp));
    }

    /**
     * Nie wołamy zewnętrznego API ip-api dla 127.0.0.1 czy 192.168.0.10. To oszczędność ip-api, i tak zwróciłby status fail
     * Każdy taki przypadek traktujemy jako niemożliwy do ustalenia
     */
    private static boolean isUnroutable(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isLoopbackAddress() || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress() || address.isAnyLocalAddress();
        } catch (UnknownHostException ex) {
            return true;
        }
    }
}
