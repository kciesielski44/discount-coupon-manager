package com.empik.coupons.infrastructure.geoip;

import com.empik.coupons.domain.CountryCode;
import com.empik.coupons.domain.GeoIp;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test pełnego stacka geo-IP
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class GeoIpIntegrationTest {

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
    private GeoIp geoIp;

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
    void successfulLookupIsCached() {
        wireMock.stubFor(get(urlPathEqualTo("/json/8.8.8.8"))
                .willReturn(okJson("{\"status\":\"success\",\"countryCode\":\"PL\"}")));

        assertThat(geoIp.lookup("8.8.8.8")).contains(new CountryCode("PL"));
        assertThat(geoIp.lookup("8.8.8.8")).contains(new CountryCode("PL"));
        assertThat(geoIp.lookup("8.8.8.8")).contains(new CountryCode("PL"));

        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/json/8.8.8.8")));
    }

    @Test
    void circuitBreakerOpensOnRepeatedFailures() {
        wireMock.stubFor(get(urlPathMatching("/json/.*"))
                .willReturn(serverError()));

        for (int i = 0; i < 10; i++) {
            assertThat(geoIp.lookup("203.0.113." + i)).isEmpty();
        }

        wireMock.resetRequests();

        assertThat(geoIp.lookup("198.51.100.1")).isEmpty();

        wireMock.verify(0, getRequestedFor(urlPathMatching("/json/.*")));
    }
}
