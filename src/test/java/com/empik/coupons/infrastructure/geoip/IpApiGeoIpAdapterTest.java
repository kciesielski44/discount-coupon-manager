package com.empik.coupons.infrastructure.geoip;

import com.empik.coupons.domain.CountryCode;
import com.empik.coupons.infrastructure.metrics.CouponMetrics;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test adaptera HTTP dla geoIP
 */
class IpApiGeoIpAdapterTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private IpApiGeoIpAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient client = RestClient.builder().baseUrl(wireMock.baseUrl()).build();
        adapter = new IpApiGeoIpAdapter(client, new CouponMetrics(new SimpleMeterRegistry()));
    }

    @Test
    void returnsCountryWhenIpApiReportsSuccess() {
        wireMock.stubFor(get(urlPathEqualTo("/json/8.8.8.8"))
                .willReturn(okJson("{\"status\":\"success\",\"countryCode\":\"PL\"}")));

        Optional<CountryCode> result = adapter.lookup("8.8.8.8");

        assertThat(result).contains(new CountryCode("PL"));
    }

    @Test
    void returnsEmptyWhenIpApiReportsFail() {
        wireMock.stubFor(get(urlPathEqualTo("/json/127.0.0.1"))
                .willReturn(okJson("{\"status\":\"fail\",\"message\":\"private range\"}")));

        Optional<CountryCode> result = adapter.lookup("127.0.0.1");

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyWhenCountryCodeFromIpApiIsNotValidIso() {
        wireMock.stubFor(get(urlPathEqualTo("/json/8.8.8.8"))
                .willReturn(okJson("{\"status\":\"success\",\"countryCode\":\"ZZ\"}")));

        Optional<CountryCode> result = adapter.lookup("8.8.8.8");

        assertThat(result).isEmpty();
    }

    @Test
    void propagatesRestClientExceptionOnServerError() {
        wireMock.stubFor(get(urlPathMatching("/json/.*"))
                .willReturn(serverError()));

        assertThatThrownBy(() -> adapter.lookup("8.8.8.8"))
                .isInstanceOf(RestClientException.class);
    }
}
