package bookdelivery.external;

import org.springframework.stereotype.Component;

@Component
public class PaymentServiceFallback implements PaymentService{

  @Override
  public void pay(Payment payment) {
    System.out.println("Circuit breaker has been opened. Fallback returned instead.");
  }

}
