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
    @Autowired OrderRepository orderRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrderTaken_UpdateOrderStatus(@Payload OrderTaken orderTaken){

        if(!orderTaken.validate()) return;

        System.out.println("\n\n##### listener UpdateOrderStatus : " + orderTaken.toJson() + "\n\n");

        // Logic //
        orderRepository.findById(orderTaken.getOrderId()).ifPresent(order->{
            orderRepository.save(order);
        }); 
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryStarted_UpdateOrderStatus(@Payload DeliveryStarted deliveryStarted){

        if(!deliveryStarted.validate()) return;

        System.out.println("\n\n##### listener UpdateOrderStatus : " + deliveryStarted.toJson() + "\n\n");

        // Logic //
        orderRepository.findById(deliveryStarted.getOrderId()).ifPresent(delivery->{
            orderRepository.save(delivery);
        }); 
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayCanceled_UpdateOrderStatus(@Payload PayCanceled payCanceled){

        if(!payCanceled.validate()) return;

        System.out.println("\n\n##### listener UpdateOrderStatus : " + payCanceled.toJson() + "\n\n");

        // Logic //
        orderRepository.findById(payCanceled.getOrderId()).ifPresent(payment->{
            orderRepository.save(payment);
        }); 
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayApproved_UpdateOrderStatus(@Payload PayApproved payApproved){

        if(!payApproved.validate()) return;

        System.out.println("\n\n##### listener UpdateOrderStatus : " + payApproved.toJson() + "\n\n");

        // Logic //
        orderRepository.findById(payApproved.getOrderId()).ifPresent(payment->{
            orderRepository.save(payment);
        }); 
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
