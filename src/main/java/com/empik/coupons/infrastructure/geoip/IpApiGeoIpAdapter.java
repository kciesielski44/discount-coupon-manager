package com.empik.coupons.infrastructure.geoip;

import com.empik.coupons.domain.CountryCode;
import com.empik.coupons.domain.GeoIp;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Adapter HTTP dla ip-api.com (darmowy do 45 req/min z jednego IP)
 *
 * Cache trzyma tylko wyniki not empty. Puste cachowałyby przejściowy błąd sieci
 */
@Component
class IpApiGeoIpAdapter implements GeoIp {

    private static final Logger log = LoggerFactory.getLogger(IpApiGeoIpAdapter.class);

    private final RestClient client;

    IpApiGeoIpAdapter(@Qualifier("geoIpRestClient") RestClient client) {
        this.client = client;
    }

    @Override
    @Cacheable(value = "geoip-lookups", unless = "#result == null")
    @CircuitBreaker(name = "geoip", fallbackMethod = "lookupFallback")
    @Retry(name = "geoip")
    public Optional<CountryCode> lookup(String ipAddress) {
        IpApiResponse response = client.get()
                .uri("/json/{ip}?fields=status,countryCode", ipAddress)
                .retrieve()
                .body(IpApiResponse.class);

        if (response == null || !response.successful()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new CountryCode(response.countryCode()));
        } catch (IllegalArgumentException ex) {
            log.warn("ip-api returned unexpected country code '{}' for {}", response.countryCode(), ipAddress);
            return Optional.empty();
        }
    }

    /**
     * Wymagana sygnatura Resilience4j. Wywyływane przez refleksję
     */
    Optional<CountryCode> lookupFallback(String ipAddress, Throwable cause) {
        log.warn("Geo-IP lookup fallback for {}: {}", ipAddress, cause.toString());
        return Optional.empty();
    }
}
