package com.empik.coupons.api;

import com.empik.coupons.domain.Coupon;
import com.empik.coupons.domain.CountryCode;
import com.empik.coupons.domain.CouponCode;
import com.empik.coupons.domain.CouponRepository;
import com.empik.coupons.domain.GeoIp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test współbieżności: 100 równoległych żądań na kuponie z maxUsages=10 musi dać dokładnie
 * 10 sukcesów i 90 odmów.
 *
 * GeoIp jest zamockowany na stałe PL dla "8.8.8.8"
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
@ActiveProfiles("test")
class CouponConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    private static final int CONCURRENT_REQUESTS = 100;
    private static final int MAX_USAGES = 10;
    private static final String COUPON_CODE = "BLACK_FRIDAY";
    private static final String CLIENT_IP = "8.8.8.8";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CouponRepository repository;

    @MockitoBean
    private GeoIp geoIp;

    @BeforeEach
    void stubGeoIp() {
        when(geoIp.lookup(any())).thenReturn(Optional.of(new CountryCode("PL")));
    }

    @Test
    void exactlyMaxUsagesSucceedUnderConcurrentLoad() throws Exception {
        createCoupon();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);

        // wszystkie wątki czekają na wspólny sygnał
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(CONCURRENT_REQUESTS);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger exhausted = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        try {
            for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                executor.submit(() -> {
                    try {
                        startGate.await();
                        HttpStatus status = HttpStatus.valueOf(useCoupon().getStatusCode().value());
                        if (status.is2xxSuccessful()) {
                            successes.incrementAndGet();
                        } else if (status == HttpStatus.CONFLICT) {
                            exhausted.incrementAndGet();
                        } else {
                            other.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finishGate.countDown();
                    }
                });
            }

            startGate.countDown();
            assertThat(finishGate.await(30, TimeUnit.SECONDS))
                    .as("all %d requests should finish within 30s", CONCURRENT_REQUESTS)
                    .isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(successes.get()).isEqualTo(MAX_USAGES);
        assertThat(exhausted.get()).isEqualTo(CONCURRENT_REQUESTS - MAX_USAGES);
        assertThat(other.get()).as("no unexpected status codes").isZero();

        Coupon finalState = repository.findByCode(new CouponCode(COUPON_CODE)).orElseThrow();
        assertThat(finalState.currentUsages()).isEqualTo(MAX_USAGES);
    }

    private void createCoupon() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{ \"code\": \"%s\", \"maxUsages\": %d, \"country\": \"PL\" }".formatted(COUPON_CODE, MAX_USAGES);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/coupons",
                new HttpEntity<>(body, headers),
                Void.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private ResponseEntity<String> useCoupon() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Forwarded-For", CLIENT_IP);
        return restTemplate.exchange(
                "/coupons/" + COUPON_CODE + "/usages",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);
    }
}
