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
    @Autowired DeliveryRepository deliveryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrderTaken_StartDelivery(@Payload OrderTaken orderTaken){

        if(!orderTaken.validate()) return;

        System.out.println("\n\n##### listener StartDelivery : " + orderTaken.toJson() + "\n\n");

        // Sample Logic //
        Delivery delivery = new Delivery();
        delivery.setOrderId(orderTaken.getOrderId());
        delivery.setCustomerName(orderTaken.getCustomerName());
        delivery.setDeliveryAddress(orderTaken.getDeliveryAddress());
        delivery.setDeliveryPhoneNumber(orderTaken.getDeliveryPhoneNumber());
        delivery.setOrderStatus(orderTaken.getOrderStatus());
        deliveryRepository.save(delivery);
    }
    
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCancelOrderTaken_CancelDelivery(@Payload CancelOrderTaken cancelOrderTaken){

        if(!cancelOrderTaken.validate()) return;

        System.out.println("\n\n##### listener CancelDelivery : " + cancelOrderTaken.toJson() + "\n\n");

        // 결제 취소시 배송도 취소(상태 UPDATE)
        deliveryRepository.findById(cancelOrderTaken.getOrderId()).ifPresent(delivery->{
            // delivery.setOrderStatus(cancelOrderTaken.getOrderStatus());
            delivery.setOrderStatus("deliveryCanceled");
            deliveryRepository.save(delivery);
        });

    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
