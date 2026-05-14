package com.empik.coupons.api;

import com.empik.coupons.application.CreateCouponCommand;
import com.empik.coupons.application.CreateCouponUseCase;
import com.empik.coupons.application.UseCouponCommand;
import com.empik.coupons.application.UseCouponUseCase;
import com.empik.coupons.domain.Coupon;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final UseCouponUseCase useCoupon;

    CouponController(CreateCouponUseCase createCoupon, UseCouponUseCase useCoupon) {
        this.createCoupon = createCoupon;
        this.useCoupon = useCoupon;
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

    @PostMapping("/{code}/usages")
    ResponseEntity<CouponResponse> use(@PathVariable String code, HttpServletRequest request) {
        // remoteAddr respektuje X-Forwarded-For tylko jeśli żądanie przeszło przez zaufane proxy
        // inaczej zwraca surowy adres klienta bez parsowania XFF
        Coupon coupon = useCoupon.use(new UseCouponCommand(code, request.getRemoteAddr()));
        return ResponseEntity.ok(CouponResponse.from(coupon));
    }
}
