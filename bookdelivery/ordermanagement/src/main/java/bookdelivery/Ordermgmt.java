package bookdelivery;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Ordermgmt_table")
public class Ordermgmt {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long orderMgmtId;
    private Long orderId;
    private Long itemId;
    private String itemName;
    private Integer qty;
    private String customerName;
    private String deliveryAddress;
    private String deliveryPhoneNumber;
    private String orderStatus;
    private Long customerId;

    @PostPersist
    public void onPostPersist(){
        OrderTaken orderTaken = new OrderTaken();
        BeanUtils.copyProperties(this, orderTaken);
        orderTaken.publishAfterCommit();

        bookdelivery.external.Coupon coupon = new bookdelivery.external.Coupon();

        coupon.setCustomerId(orderTaken.getCustomerId());
        coupon.setOrderId(orderTaken.getOrderId());
        coupon.setCouponStatus("sending");
        OrdermanagementApplication.applicationContext.getBean(bookdelivery.external.CouponService.class)
            .sendCoupon(coupon);

    }

    @PostUpdate
    public void onPostUpdate(){
        CancelOrderTaken cancelOrderTaken = new CancelOrderTaken();
        BeanUtils.copyProperties(this, cancelOrderTaken);
        cancelOrderTaken.publishAfterCommit();


    }


    public Long getOrderMgmtId() {
        return orderMgmtId;
    }

    public void setOrderMgmtId(Long orderMgmtId) {
        this.orderMgmtId = orderMgmtId;
    }
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
    public String getDeliveryPhoneNumber() {
        return deliveryPhoneNumber;
    }

    public void setDeliveryPhoneNumber(String deliveryPhoneNumber) {
        this.deliveryPhoneNumber = deliveryPhoneNumber;
    }
    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }




}
