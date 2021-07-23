package bookdelivery;

import bookdelivery.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired OrdermgmtRepository ordermgmtRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayApproved_TakeOrderInfo(@Payload PayApproved payApproved){

        if(!payApproved.validate()) return;

        System.out.println("\n\n##### listener TakeOrderInfo : " + payApproved.toJson() + "\n\n");

        // Sample Logic //
        // Ordermgmt ordermgmt = new Ordermgmt();
        // ordermgmtRepository.save(ordermgmt);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrderCanceled_CancelOrder(@Payload OrderCanceled orderCanceled){

        if(!orderCanceled.validate()) return;

        System.out.println("\n\n##### listener CancelOrder : " + orderCanceled.toJson() + "\n\n");
        
        // 결제 취소시 상태 UPDATE 필요
        ordermgmtRepository.findByOrderId(orderCanceled.getOrderId()).ifPresent(order->{
            order.setOrderStatus("orderCanceled");//add
            ordermgmtRepository.save(order);
        }); 
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
