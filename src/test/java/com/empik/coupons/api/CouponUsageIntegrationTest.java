package com.empik.coupons.api;

import com.empik.coupons.domain.CountryCode;
import com.empik.coupons.domain.GeoIp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test integracyjny scenariusza pozytywnego: kontroler -> use case -> adapter -> Postgres.
 * GeoIp jest zamockowany, żeby nie wołać prawdziwego ip-api z testów
 * Adres klienta wstrzykiwany przez setRemoteAddr
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class CouponUsageIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeoIp geoIp;

    @BeforeEach
    void stubGeoIp() {
        when(geoIp.lookup(any())).thenReturn(Optional.of(new CountryCode("PL")));
    }

    @Test
    void registersUsageOnExistingCoupon() throws Exception {
        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"code\": \"WIOSNA_2026\", \"maxUsages\": 5, \"country\": \"PL\" }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentUsages").value(0));

        mockMvc.perform(post("/coupons/WIOSNA_2026/usages")
                        .with(request -> { request.setRemoteAddr("8.8.8.8"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("wiosna_2026"))
                .andExpect(jsonPath("$.currentUsages").value(1))
                .andExpect(jsonPath("$.maxUsages").value(5))
                .andExpect(jsonPath("$.country").value("PL"));

        mockMvc.perform(post("/coupons/wiosna_2026/usages")
                        .with(request -> { request.setRemoteAddr("8.8.8.8"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentUsages").value(2));
    }
}
