package com.empik.coupons.api;

import com.empik.coupons.application.CreateCouponCommand;
import com.empik.coupons.application.CreateCouponUseCase;
import com.empik.coupons.domain.Coupon;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/coupons")
class CouponController {

    private final CreateCouponUseCase createCoupon;

    CouponController(CreateCouponUseCase createCoupon) {
        this.createCoupon = createCoupon;
    }

    @PostMapping
    ResponseEntity<CouponResponse> create(@Valid @RequestBody CreateCouponRequest request) {
        Coupon coupon = createCoupon.create(
                new CreateCouponCommand(
                        request.code(),
                        request.maxUsages(),
                        request.country()
                )
        );

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{code}")
                .buildAndExpand(coupon.code().value())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(CouponResponse.from(coupon));
    }
}
