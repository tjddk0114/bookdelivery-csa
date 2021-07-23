package bookdelivery;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Coupon_table")
public class Coupon {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long couponCode;
    private Long customerId;
    private Long orderId;
    private String couponStatus;

    @PostPersist
    public void onPostPersist(){
        if(this.couponStatus.equals("sending")){
            CouponSent couponSent = new CouponSent();  
            BeanUtils.copyProperties(this, couponSent);
            couponSent.setCouponStatus("valid");
            couponSent.publishAfterCommit();
        }

    }
    @PostUpdate
    public void onPostUpdate(){
        CouponCanceled couponCanceled = new CouponCanceled();
        BeanUtils.copyProperties(this, couponCanceled);
        couponCanceled.publishAfterCommit();

    }
    @PrePersist
    public void onPrePersist(){
        try{
            Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
           e.printStackTrace();
        }
    }

    public Long getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(Long couponCode) {
        this.couponCode = couponCode;
    }
    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public String getCouponStatus() {
        return couponStatus;
    }

    public void setCouponStatus(String couponStatus) {
        this.couponStatus = couponStatus;
    }




}
