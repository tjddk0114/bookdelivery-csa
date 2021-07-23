package bookdelivery;

import bookdelivery.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class MyCouponViewHandler {


    @Autowired
    private MyCouponRepository myCouponRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenCouponSent_then_CREATE_1 (@Payload CouponSent couponSent) {
        try {

            if (!couponSent.validate()) return;

            // view 객체 생성
            MyCoupon myCoupon = new MyCoupon();
            // view 객체에 이벤트의 Value 를 set 함
            myCoupon.setCouponCode(couponSent.getCouponCode());
            myCoupon.setCustomerId(couponSent.getCustomerId());
            myCoupon.setCouponStatus(couponSent.getCouponStatus());
            // view 레파지 토리에 save
            myCouponRepository.save(myCoupon);

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenCouponCanceled_then_UPDATE_1(@Payload CouponCanceled couponCanceled) {
        try {
            if (!couponCanceled.validate()) return;
                // view 객체 조회
            Optional<MyCoupon> myCouponOptional = myCouponRepository.findByCouponCode(couponCanceled.getCouponCode());

            if( myCouponOptional.isPresent()) {
                 MyCoupon myCoupon = myCouponOptional.get();
            // view 객체에 이벤트의 eventDirectValue 를 set 함
                 myCoupon.setCouponStatus(couponCanceled.getCouponStatus());
                // view 레파지 토리에 save
                 myCouponRepository.save(myCoupon);
                }


        }catch (Exception e){
            e.printStackTrace();
        }
    }

}

