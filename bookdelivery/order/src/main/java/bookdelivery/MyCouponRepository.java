package bookdelivery;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface MyCouponRepository extends CrudRepository<MyCoupon, Long> {

    Optional<MyCoupon> findByCouponCode(Long couponCode);

    
}