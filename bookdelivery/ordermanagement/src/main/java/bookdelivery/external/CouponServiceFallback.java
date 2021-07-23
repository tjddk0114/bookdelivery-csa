package bookdelivery.external;

import org.springframework.stereotype.Component;

@Component
public class CouponServiceFallback implements CouponService{

  @Override
  public void sendCoupon(Coupon coupon) {
    System.out.println("Circuit breaker has been opened. Fallback returned instead.");
  }

}
