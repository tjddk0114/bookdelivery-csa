package bookdelivery;

import bookdelivery.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class PolicyHandler{
    @Autowired CouponRepository couponRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayCanceled_CancelCoupon(@Payload PayCanceled payCanceled){

        if(!payCanceled.validate()) return;

        System.out.println("\n\n##### listener CancelCoupon : " + payCanceled.toJson() + "\n\n");

        couponRepository.findByOrderId(payCanceled.getOrderId()).ifPresent(coupon->{
            coupon.setCouponStatus("invalid");
            couponRepository.save(coupon);
        }); 



        // Sample Logic //
        // Coupon coupon = new Coupon();
        // couponRepository.save(coupon);

    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
