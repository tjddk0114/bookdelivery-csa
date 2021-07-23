package bookdelivery;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="MyCoupon_table")
public class MyCoupon {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long myCouponId;
        private Long couponCode;
        private Long customerId;
        private String couponStatus;

        public Long getMyCouponId() {
            return myCouponId;
        }

        public void setMyCouponId(Long myCouponId) {
            this.myCouponId = myCouponId;
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
        public String getCouponStatus() {
            return couponStatus;
        }

        public void setCouponStatus(String couponStatus) {
            this.couponStatus = couponStatus;
        }

}
