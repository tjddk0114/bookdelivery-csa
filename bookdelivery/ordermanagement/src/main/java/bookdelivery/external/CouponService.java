
package bookdelivery.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

//운영에서는 url="http://coupon:8080"

@FeignClient(name="coupon", url="http://localhost:8085", fallback = CouponServiceFallback.class)
public interface CouponService {

    @RequestMapping(method= RequestMethod.POST, path="/coupons")
    public void sendCoupon(@RequestBody Coupon coupon);

}