package com.empik.coupons.api;

import com.empik.coupons.application.CreateCouponCommand;
import com.empik.coupons.application.CreateCouponUseCase;
import com.empik.coupons.application.UseCouponCommand;
import com.empik.coupons.application.UseCouponUseCase;
import com.empik.coupons.domain.CountryCode;
import com.empik.coupons.domain.Coupon;
import com.empik.coupons.domain.CouponCode;
import com.empik.coupons.domain.CouponCodeAlreadyExistsException;
import com.empik.coupons.domain.CouponExhaustedException;
import com.empik.coupons.domain.CouponNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test warstwy HTTP (kontroler wraz z mapowaniem wyjątków)
 * Use case jest zmockowany
 */
@WebMvcTest(CouponController.class)
@Import(CouponExceptionAdvice.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCouponUseCase createCoupon;

    @MockitoBean
    private UseCouponUseCase useCoupon;

    @Test
    void createsCouponAndReturns201WithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-13T10:15:30Z");
        when(createCoupon.create(any(CreateCouponCommand.class)))
                .thenReturn(new Coupon(
                        id,
                        new CouponCode("wiosna"),
                        createdAt,
                        100,
                        0,
                        new CountryCode("PL")
                ));

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"code\": \"WIOSNA\", \"maxUsages\": 100, \"country\": \"PL\" }"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/coupons/wiosna"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("wiosna"))
                .andExpect(jsonPath("$.createdAt").value("2026-05-13T10:15:30Z"))
                .andExpect(jsonPath("$.maxUsages").value(100))
                .andExpect(jsonPath("$.currentUsages").value(0))
                .andExpect(jsonPath("$.country").value("PL"));

        verify(createCoupon).create(new CreateCouponCommand("WIOSNA", 100, "PL"));
    }

    @Test
    void returns400WhenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"code\": \"\", \"maxUsages\": -5, \"country\": \"\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns409WhenCouponCodeAlreadyExists() throws Exception {
        when(createCoupon.create(any(CreateCouponCommand.class)))
                .thenThrow(new CouponCodeAlreadyExistsException(new CouponCode("wiosna")));

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"code\": \"wiosna\", \"maxUsages\": 10, \"country\": \"PL\" }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPON_CODE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void usesCouponAndReturns200WithUpdatedState() throws Exception {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-13T10:15:30Z");
        when(useCoupon.use(any(UseCouponCommand.class)))
                .thenReturn(new Coupon(
                        id,
                        new CouponCode("wiosna"),
                        createdAt,
                        100,
                        1,
                        new CountryCode("PL")
                ));

        mockMvc.perform(post("/coupons/WIOSNA/usages"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("wiosna"))
                .andExpect(jsonPath("$.currentUsages").value(1));

        verify(useCoupon).use(new UseCouponCommand("WIOSNA"));
    }

    @Test
    void returns404WhenCouponDoesNotExist() throws Exception {
        when(useCoupon.use(any(UseCouponCommand.class)))
                .thenThrow(new CouponNotFoundException(new CouponCode("missing")));

        mockMvc.perform(post("/coupons/missing/usages"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COUPON_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void returns409WhenCouponIsExhausted() throws Exception {
        when(useCoupon.use(any(UseCouponCommand.class)))
                .thenThrow(new CouponExhaustedException(new CouponCode("wiosna")));

        mockMvc.perform(post("/coupons/wiosna/usages"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPON_EXHAUSTED"))
                .andExpect(jsonPath("$.message").exists());
    }
}
