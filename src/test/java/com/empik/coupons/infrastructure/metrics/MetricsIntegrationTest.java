package com.empik.coupons.infrastructure.metrics;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Test metryk biznesowych
 * Każdy scenariusz biznesowy (sukces, wyczerpany, nieznaleziony, blokada krajem,
 * niemożliwy do ustalenia kraj) inkrementuje dedykowany counter, a wywołania geoip dorzucają próbki do timera
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class MetricsIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void overrideGeoIpUrl(DynamicPropertyRegistry registry) {
        registry.add("app.geoip.base-url", wireMock::baseUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetState() {
        cacheManager.getCache("geoip-lookups").clear();
        circuitBreakerRegistry.circuitBreaker("geoip").reset();
        wireMock.resetAll();
    }

    @Test
    void incrementsBusinessCountersAndGeoIpTimer() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/json/8.8.8.8"))
                .willReturn(okJson("{\"status\":\"success\",\"countryCode\":\"PL\"}")));
        wireMock.stubFor(get(urlPathEqualTo("/json/1.1.1.1"))
                .willReturn(okJson("{\"status\":\"success\",\"countryCode\":\"DE\"}")));
        wireMock.stubFor(get(urlPathEqualTo("/json/9.9.9.9"))
                .willReturn(serverError()));

        double createdBefore = counterValue("coupon.created");
        double successBefore = counterValue("coupon.usage.attempts", "result", "success");
        double exhaustedBefore = counterValue("coupon.usage.attempts", "result", "exhausted");
        double notFoundBefore = counterValue("coupon.usage.attempts", "result", "not_found");
        double blockedBefore = counterValue("coupon.usage.attempts", "result", "country_blocked");
        double unresolvedBefore = counterValue("coupon.usage.attempts", "result", "country_unresolved");
        long geoSuccessBefore = timerCount("geoip.lookup", "outcome", "success");
        long geoErrorBefore = timerCount("geoip.lookup", "outcome", "error");

        createCoupon("metrics_a", 1);
        useCoupon("metrics_a", "8.8.8.8");   // success
        useCoupon("metrics_a", "8.8.8.8");   // exhausted (kupon zużyty)

        createCoupon("metrics_b", 5);
        useCoupon("metrics_b", "1.1.1.1");   // countryBlocked (kupon PL, klient DE)

        createCoupon("metrics_c", 5);
        useCoupon("metrics_c", "127.0.0.1"); // countryUnresolved (loopback, bez wywołania ip-api)
        useCoupon("metrics_c", "9.9.9.9");   // countryUnresolved (ip-api 5xx, fallback na empty)

        useCoupon("missing_coupon", "8.8.8.8"); // notFound

        assertThat(counterValue("coupon.created") - createdBefore).isEqualTo(3.0);
        assertThat(counterValue("coupon.usage.attempts", "result", "success") - successBefore).isEqualTo(1.0);
        assertThat(counterValue("coupon.usage.attempts", "result", "exhausted") - exhaustedBefore).isEqualTo(1.0);
        assertThat(counterValue("coupon.usage.attempts", "result", "not_found") - notFoundBefore).isEqualTo(1.0);
        assertThat(counterValue("coupon.usage.attempts", "result", "country_blocked") - blockedBefore).isEqualTo(1.0);
        assertThat(counterValue("coupon.usage.attempts", "result", "country_unresolved") - unresolvedBefore).isEqualTo(2.0);

        assertThat(timerCount("geoip.lookup", "outcome", "success") - geoSuccessBefore).isGreaterThanOrEqualTo(2L);
        assertThat(timerCount("geoip.lookup", "outcome", "error") - geoErrorBefore).isGreaterThanOrEqualTo(1L);
    }

    private void createCoupon(String code, int maxUsages) throws Exception {
        mockMvc.perform(post("/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"maxUsages\":" + maxUsages + ",\"country\":\"PL\"}"));
    }

    private void useCoupon(String code, String ip) throws Exception {
        mockMvc.perform(post("/coupons/" + code + "/usages")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                }));
    }

    private double counterValue(String name) {
        var counter = registry.find(name).counter();
        return counter == null ? 0.0 : counter.count();
    }

    private double counterValue(String name, String tagKey, String tagValue) {
        Search search = registry.find(name).tag(tagKey, tagValue);
        var counter = search.counter();
        return counter == null ? 0.0 : counter.count();
    }

    private long timerCount(String name, String tagKey, String tagValue) {
        var timer = registry.find(name).tag(tagKey, tagValue).timer();
        return timer == null ? 0L : timer.count();
    }
}
