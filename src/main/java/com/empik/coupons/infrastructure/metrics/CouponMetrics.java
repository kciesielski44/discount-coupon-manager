package com.empik.coupons.infrastructure.metrics;

import com.empik.coupons.domain.CouponErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Centralny rejestr metryk biznesowych
 */
@Component
public class CouponMetrics {

    private static final String COUPON_CREATED = "coupon.created";
    private static final String USAGE_ATTEMPTS = "coupon.usage.attempts";
    private static final String GEOIP_LOOKUP = "geoip.lookup";

    private final MeterRegistry registry;
    private final Counter couponCreated;

    public CouponMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.couponCreated = Counter.builder(COUPON_CREATED)
                .description("Number of successfully created coupons")
                .register(registry);
    }

    public void recordCouponCreated() {
        couponCreated.increment();
    }

    public void recordUsageAttempt(UsageResult result) {
        registry.counter(USAGE_ATTEMPTS, "result", result.tag).increment();
    }

    public Timer.Sample startGeoIpSample() {
        return Timer.start(registry);
    }

    public void stopGeoIpSample(Timer.Sample sample, GeoIpOutcome outcome) {
        sample.stop(registry.timer(GEOIP_LOOKUP, "outcome", outcome.tag));
    }

    /**
     * Wyniki próby użycia kuponu
     */
    public enum UsageResult {
        SUCCESS("success"),
        NOT_FOUND("not_found"),
        EXHAUSTED("exhausted"),
        COUNTRY_BLOCKED("country_blocked"),
        COUNTRY_UNRESOLVED("country_unresolved");

        private final String tag;

        UsageResult(String tag) {
            this.tag = tag;
        }

        public static UsageResult fromErrorCode(CouponErrorCode errorCode) {
            return switch (errorCode) {
                case COUPON_NOT_FOUND -> NOT_FOUND;
                case COUPON_EXHAUSTED -> EXHAUSTED;
                case COUNTRY_NOT_ALLOWED -> COUNTRY_BLOCKED;
                case CLIENT_COUNTRY_UNRESOLVED -> COUNTRY_UNRESOLVED;
                case COUPON_CODE_ALREADY_EXISTS ->
                        throw new IllegalArgumentException("COUPON_CODE_ALREADY_EXISTS to błąd tworzenia, nie użycia");
            };
        }
    }

    /**
     * Wynik pojedynczego wywołania ip-api
     */
    public enum GeoIpOutcome {
        SUCCESS("success"),
        ERROR("error");

        private final String tag;

        GeoIpOutcome(String tag) {
            this.tag = tag;
        }
    }
}
