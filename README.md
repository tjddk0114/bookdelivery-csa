# bookdelivery-csa
Lv.2 Intensive Coursework

<img src="https://user-images.githubusercontent.com/85722733/126579042-af1eaaeb-909e-4ec6-a8e6-cc4ab3d80ef2.png"  width="50%" height="50%">

# 온라인 도서상점 (도서배송 서비스) (3조 - 조성아 개인평가)

# Table of contents

- [조별과제 - 도서배송 서비스](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현)
    - [DDD 의 적용](#DDD-의-적용)
    - [동기식 호출과 Fallback 처리](#동기식-호출과-Fallback-처리)
    - [비동기식 호출과 Eventual Consistency](#비동기식-호출과-Eventual-Consistency)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [API 게이트웨이](#API-게이트웨이)
  - [운영](#운영)
    - [Deploy/Pipeline](#deploypipeline)
    - [동기식 호출 / Circuit Breaker / 장애격리](#동기식-호출-circuit-breaker-장애격리)
    - [Autoscale (HPA)](#Autoscale-(HPA))
    - [Zero-downtime deploy (Readiness Probe)](#Zerodowntime-deploy-(Readiness-Probe))
    - [ConfigMap](#ConfigMap)
    - [Self-healing (Liveness Probe)](#self-healing-(liveness-probe))


# 서비스 시나리오

기능적 요구사항
1. 고객이 도서를 선택하여 주문(Order)한다
2. 고객이 결제(Pay)한다
3. 결제가 완료되면 주문 내역이 도서상점에 전달된다(Ordermanagement)
4. 상점주인이 주문을 접수함과 동시에 도서를 포장한다
5. 상점주인은 주문 접수 시 고객에게 도서할인쿠폰을 발행할 수 있다(Coupon) (추가)
6. 도서 포장이 완료되면 상점소속배달기사가 배송(Delivery)을 시작한다
7. 고객이 주문을 취소할 수 있다
8. 주문이 취소되면 배송 및 결제가 취소된다
9. 결제 취소 시 해당 주문으로 발행된 쿠폰이 있다면 해당 쿠폰이 무효화된다 (추가)
10. 고객이 주문상태를 중간중간 조회한다
11. 주문/배송상태가 바뀔 때마다 고객이 마이페이지에서 상태를 확인할 수 있다
12. 고객이 마이쿠폰 페이지에서 본인에게 발행된 쿠폰을 확인할 수 있다 (추가)

비기능적 요구사항
1. 트랜잭션
  - 결제가 완료되어야만 주문이 완료된다 (결제가 되지 않은 주문건은 아예 거래가 성립되지 않아야 한다 Sync 호출)
  - 상점주인이 주문 접수를 해야지만 쿠폰을 발행할 수 있다 (Sync 호출) (추가)
2. 장애격리
  - 배송관리(Delivery) 기능이 수행되지 않더라도 주문접수(Ordermanagement)는 365일 24시간 가능해야 한다 Async (event-driven), Eventual Consistency 
  - 결제시스템이 과중되면 사용자를 잠시동안 받지 않고 결제를 잠시후에 하도록 유도한다 Circuit breaker, fallback
  - 쿠폰시스템이 과중되면 쿠폰 발행 요청을 잠시동안 받지 않고 잠시후에 발행하도록 유도한다 Circuit breaker, fallback (추가)
3. 성능
  - 고객이 마이페이지에서 배송상태를 확인할 수 있어야 한다 CQRS
  - 고객이 본인에게 발행된 쿠폰을 마이쿠폰페이지에서 확인할 수 있어야 한다 CQRS (추가)


# 체크포인트

- 분석 설계


  - 이벤트스토밍: 
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    

  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?

  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?

  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?


# 분석/설계


## AS-IS 조직 (Horizontally-Aligned)
  ![image](https://user-images.githubusercontent.com/487999/79684144-2a893200-826a-11ea-9a01-79927d3a0107.png)

## TO-BE 조직 (Vertically-Aligned)
  <img src="https://user-images.githubusercontent.com/85722733/124564081-a9708780-de7b-11eb-93aa-42c819be9059.png"  width="80%" height="80%">

## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/null/348fa7c90636e01a5525272b163ef307
* 3조 조성아 개인 모델링 결과: http://www.msaez.io/#/storming/2WHhr58frxP61Pm7rSPRUu6bGAY2/a3d0c571347ffe13b35a5adaf281728d

### 이벤트 도출
<img src="https://user-images.githubusercontent.com/85722733/126592134-afa1e97d-fced-4931-8e70-8014e55978c2.png"  width="80%" height="80%">

### 부적격 이벤트 탈락
<img src="https://user-images.githubusercontent.com/85722733/126592182-92ea9f11-2036-4fe0-869d-a90fcdd92e11.png"  width="80%" height="80%">

    - 과정중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함
        - '쿠폰취소요청됨'은 이벤트에 의한 반응에 가까우므로 폴리시로 변경하고 이벤트에서 제외
        - '쿠폰발행내역이 조회됨'은 발생한 사실, 결과라고 보기 어려우므로 View로 변경하고 이벤트에서 제외

### 액터, 커맨드 부착하여 읽기 좋게
<img src="https://user-images.githubusercontent.com/85722733/126592198-385de8c4-bdb8-4bdb-ad54-2558c779f649.png"  width="65%" height="65%">

### 어그리게잇으로 묶기
<img src="https://user-images.githubusercontent.com/85722733/126592215-3624624e-981a-4cb9-ba26-7707b9d1a073.png"  width="80%" height="80%">

    - 고객의 주문, 상점의 주문관리, 결제의 결제이력, 배송의 배송이력, 쿠폰의 쿠폰발행이력은 그와 연결된 command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그들끼리 묶어줌

### 바운디드 컨텍스트로 묶기

<img src="https://user-images.githubusercontent.com/85722733/126592234-4e74761f-bcf3-4b7f-99ce-bc676a278008.png"  width="80%" height="80%">

    - 도메인 서열 분리 
        - Core Domain:  order, ordermanagement : 없어서는 안될 핵심 서비스이며, 연견 Up-time SLA 수준을 99.999% 목표, 배포주기는 order의 경우 1주일 1회 미만, ordermanagement의 경우 1개월 1회 미만
        - Supporting Domain:  delivery, coupon : 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기는 각 팀의 자율이나 표준 스프린트 주기가 1주일 이므로 1주일 1회 이상을 기준으로 함
        - General Domain:   pay : 결제서비스로 3rd Party 외부 서비스를 사용하는 것이 경쟁력이 높음 (핑크색으로 이후 전환할 예정)

### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)

<img src="https://user-images.githubusercontent.com/85722733/126592251-99aae8c7-462a-438e-b5ce-54e35ac63bf3.png"  width="80%" height="80%">

### 폴리시의 이동과 컨텍스트 매핑(점선은 Pub/Sub, 실선은 Req/Resp)을 통해 완성된 모형

<img src="https://user-images.githubusercontent.com/85722733/126592292-cdf1718e-ac9a-4a02-8c62-d43702e74f56.png"  width="90%" height="90%">

### 완성본에 대한 기능적/비기능적 요구사항을 커버하는지 검증

<img src="https://user-images.githubusercontent.com/85722733/126592326-3e84a1e3-b100-428d-a9b1-861df5b411e1.png"  width="90%" height="90%">

    - 고객이 도서를 선택하여 주문한다 (ok)
    - 고객이 결제한다 (ok)
    - 결제가 완료되면 주문 내역이 도서상점에 전달된다 (ok)
    - 상점주인이 주문을 접수함과 동시에 도서를 포장한다 (ok)
    - 상점주인은 주문 접수 시 고객에게 도서할인쿠폰을 발행할 수 있다 (ok)
    - 도서 포장이 완료되면 상점소속배달기사가 배송을 시작한다 (ok)

<img src="https://user-images.githubusercontent.com/85722733/126592351-98d1147e-fc4e-4fa8-bf74-9809802bc4d6.png"  width="90%" height="90%">
 
    - 고객이 주문을 취소할 수 있다 (ok)
    - 주문이 취소되면 배송 및 결제가 취소된다 (ok)
    - 결제 취소 시 해당 주문으로 발행된 쿠폰이 있다면 해당 쿠폰이 무효화된다 (ok)
    - 고객이 주문상태를 중간중간 조회한다 (ok)
    - 주문/배송상태가 바뀔 때마다 고객이 마이페이지에서 상태를 확인할 수 있다 (ok)
    - 고객이 마이쿠폰 페이지에서 본인에게 발행된 쿠폰을 확인할 수 있다 (ok)


### 비기능 요구사항에 대한 검증
<img src="https://user-images.githubusercontent.com/85722733/126592370-332e796c-d115-453d-82a1-4ff8a9c414c1.png"  width="90%" height="90%">

    - 마이크로서비스를 넘나드는 시나리오에 대한 트랜잭션 처리
        - 고객 주문시 결제처리:  결제가 완료되지 않은 주문은 절대 받지 않는다는 경영자의 오랜 신념(?)에 따라, ACID 트랜잭션 적용. 주문완료시 결제처리에 대해서는 Request-Response 방식 처리
        - 주문 접수시 쿠폰발행처리: 주문접수된 건의 고객만 쿠폰발행 대상이 될 수 있도록 주문접수완료 시 쿠폰발행처리에 대해서는 Request-Response 방식 처리
	- 결제 완료시 점주연결 및 배송처리:  payment 에서 ordermanagement 마이크로서비스로 주문요청이 전달되는 과정에 있어서 ordermanagement 마이크로서비스가 별도의 배포주기를 가지기 때문에 Eventual Consistency 방식으로 트랜잭션 처리함.
        - 나머지 모든 inter-microservice 트랜잭션: 데이터 일관성의 시점이 크리티컬하지 않은 모든 경우가 대부분이라 판단, Eventual Consistency 를 기본으로 채택함.


## 헥사고날 아키텍처 다이어그램 도출
    
![헥사고날아키텍쳐](https://user-images.githubusercontent.com/85722733/125288478-29ee2700-e359-11eb-93f0-acdc66789152.png)

신규 서비스인 쿠폰(Coupon) 서비스 추가
![헥사고날아키텍쳐_쿠폰추가](https://user-images.githubusercontent.com/85722733/126592416-e4c14032-113a-4a0b-98d7-a0bef74bc3f0.png)

    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 Pub/Sub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐

# 구현 

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 바운더리 컨텍스트 별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
cd order
mvn spring-boot:run

cd payment
mvn spring-boot:run 

cd ordermanagement
mvn spring-boot:run  

cd delivery
mvn spring-boot:run 

cd coupon
mvn spring-boot:run 

cd gateway
mvn spring-boot:run 
```

## DDD 의 적용

- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가? 

각 서비스 내에 도출된 핵심 Aggregate Root 객체를 Entity로 선언하였다. (주문(order), 결제(payment), 주문관리(ordermgmt), 배송(delivery), 쿠폰(coupon))

쿠폰 Entity (Coupon.java)
```
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

```

Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 하였고 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다 

CouponRepository.java
```
package bookdelivery;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="coupons", path="coupons")
public interface CouponRepository extends PagingAndSortingRepository<Coupon, Long>{

    Optional<Coupon> findByOrderId(Long orderId);

}
```

- 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?

가능한 현업에서 사용하는 언어(유비쿼터스 랭귀지)를 모델링 및 구현 시 그대로 사용하려고 노력하였다.

- 적용 후 Rest API의 테스트

order 주문하기 POST
```
http POST localhost:8088/orders customerId=7777 customerName="HeidiCho" itemId=123 itemName="ITbook" qty=3 itemPrice=1000 deliveryAddress="kyungkido sungnamsi" deliveryPhoneNumber="01012341234" orderStatus="orderPlaced"
```
![주문생성api](https://user-images.githubusercontent.com/85722733/126617513-e2689f6b-9cc2-46ff-8dbe-4c23389ebd5b.png)

order 주문 취소하기 PATCH 
```
http PATCH localhost:8088/orders/1 orderStatus="orderCanceled"
```
![주문취소api](https://user-images.githubusercontent.com/85722733/126617548-bfdaa293-6806-4804-b612-31335a17a1e6.png)


## 동기식 호출과 Fallback 처리 
(Request-Response 방식의 서비스 중심 아키텍처 구현)

- 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)

요구사항대로 점주가 주문 접수를 해야지만 쿠폰 발행 서비스를 호출할 수 있도록 주문 접수 시 쿠폰 발행 처리를 동기식으로 호출하도록 한다. 

Ordermgmt.java Entity Class에 @PostPersist로 주문 접수 직후 쿠폰 발행을 호출하도록 처리하였다
```
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
```
동기식 호출은 CouponService 클래스를 두어 FeignClient 를 이용하여 호출하도록 하였다.

CouponService.java

```
package bookdelivery.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="coupon", url="http://localhost:8085")
public interface CouponService {

    @RequestMapping(method= RequestMethod.POST, path="/coupons")
    public void sendCoupon(@RequestBody Coupon coupon);

}
```
동기식 호출로 인하여, 쿠폰발행 서비스에 장애 발생 시(서비스 다운) 주문관리 서비스에도 장애가 전파된다는 것을 확인
```
Ordermanagement 서비스 구동 & Coupon 서비스 다운 되어 있는 상태에서는 주문접수 시 오류 발생

PS C:\> http POST localhost:8088/ordermgmts orderId=7 customerId=7779 itemName="ITbook" qty=3 customerName="Jenna" deliveryAddress="Gyeonggido Sungnamsi" deliveryPhoneNumber="01012341234" orderStatus="orderTaken"
HTTP/1.1 500 Internal Server Error
Content-Type: application/json;charset=UTF-8
Date: Thu, 22 Jul 2021 09:04:21 GMT
transfer-encoding: chunked

{
    "error": "Internal Server Error",
    "message": "Could not commit JPA transaction; nested exception is javax.persistence.RollbackException: Error while committing the transaction",
    "path": "/ordermgmts",
    "status": 500,
    "timestamp": "2021-07-22T09:04:21.479+0000"
}

--> Coupon 서비스 구동
C:\workspace\bookdelivery\coupon>mvn spring-boot:run

--> 주문접수 재생성 시 정상적으로 생성됨
PS C:\> http POST localhost:8088/ordermgmts orderId=7 customerId=7779 itemName="ITbook" qty=3 customerName="Jenna" deliveryAddress="Gyeonggido Sungnamsi" deliveryPhoneNumber="01012341234" orderStatus="orderTaken"
HTTP/1.1 201 Created
Content-Type: application/json;charset=UTF-8
Date: Thu, 22 Jul 2021 09:12:21 GMT
Location: http://localhost:8082/ordermgmts/8
transfer-encoding: chunked

{
    "_links": {
        "ordermgmt": {
            "href": "http://localhost:8082/ordermgmts/8"
        },
        "self": {
            "href": "http://localhost:8082/ordermgmts/8"
        }
    },
    "customerId": 7779,
    "customerName": "Jenna",
    "deliveryAddress": "Gyeonggido Sungnamsi",
    "deliveryPhoneNumber": "01012341234",
    "itemId": null,
    "itemName": "ITbook",
    "orderId": 7,
    "orderStatus": "orderTaken",
    "qty": 3
}
```

- 서킷브레이커를 통하여 장애를 격리시킬 수 있는가?

주문접수-쿠폰발행 Req-Res구조에서 FeignClient 및 Spring Hystrix 를 사용하여 Fallback 기능을 구현하였다

Ordermanagement 서비스의 application.yml 파일에 feign.hystrix.enabled: true 로 활성화시킨다

```
feign:
  hystrix:
    enabled: true
```
CouponService 에 feignClient fallback 옵션을 추가하였고 이를 위해 CouponServiceFallback 클래스를 추가하였다

CouponService.java
```
package bookdelivery.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="coupon", url="http://localhost:8085", fallback = CouponServiceFallback.class)
public interface CouponService {

    @RequestMapping(method= RequestMethod.POST, path="/coupons")
    public void sendCoupon(@RequestBody Coupon coupon);

}
```
CouponServiceFallback.java
```
package bookdelivery.external;

import org.springframework.stereotype.Component;

@Component
public class CouponServiceFallback implements CouponService{

  @Override
  public void sendCoupon(Coupon coupon) {
    System.out.println("Circuit breaker has been opened. Fallback returned instead.");
  }

}

```
fallback 기능 없이 coupon 서비스를 중지하고 주문접수 생성 시에는 오류가 발생했으나, 

위와 같이 fallback 기능 활성화 후에는 coupon 서비스가 동작하지 않더라도 주문접수 생성 시에 오류가 발생하지 않는다

```
PS C:\> http POST localhost:8088/ordermgmts orderId=13 customerId=7781 itemName="ITbook" qty=3 customerName="BJ" deliveryAddress="Gyeonggido Sungnamsi" deliveryPhoneNumber="01012341234" orderStatus="orderTaken"
HTTP/1.1 201 Created
Content-Type: application/json;charset=UTF-8
Date: Thu, 22 Jul 2021 09:22:22 GMT
Location: http://localhost:8082/ordermgmts/2
transfer-encoding: chunked

{
    "_links": {
        "ordermgmt": {
            "href": "http://localhost:8082/ordermgmts/2"
        },
        "self": {
            "href": "http://localhost:8082/ordermgmts/2"
        }
    },
    "customerId": 7781,
    "customerName": "BJ",
    "deliveryAddress": "Gyeonggido Sungnamsi",
    "deliveryPhoneNumber": "01012341234",
    "itemId": null,
    "itemName": "ITbook",
    "orderId": 13,
    "orderStatus": "orderTaken",
    "qty": 3
}
```
![주문접수-서킷브레이커](https://user-images.githubusercontent.com/85722733/126617386-3513649c-12ec-4d3b-a368-e3f515f32822.png)

위와 같이 ordermanagement 서비스에서 fallack 옵션이 동작하여 "Circuit breaker has been opened. Fallback returned instead." 로그가 보여진다


## 비동기식 호출과 Eventual Consistency 
(이벤트 드리븐 아키텍처)

- 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?

- Correlation-key: 각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?

카프카를 이용하여 주문완료 시 결제 처리 및 주문접수 시 쿠폰발행 처리를 제외한 나머지 모든 마이크로서비스 트랜잭션은 Pub/Sub 관계로 구현하였다. 

아래는 결제취소 이벤트(PayCanceled)를 카프카를 통해 쿠폰(coupon) 서비스에 연계받는 코드 내용이다. 

payment 서비스에서는 고객의 주문취소 -> 점주의 주문접수취소 시 PostUpdate로 PayCanceled 이벤트를 발생시키고,
```
public class Payment {
    @PostUpdate
    public void onPostUpdate(){
        PayCanceled payCanceled = new PayCanceled();
        BeanUtils.copyProperties(this, payCanceled);
        payCanceled.publishAfterCommit();
    }
```

coupon 서비스에서는 카프카 리스너를 통해 payment PayCanceled 이벤트를 수신받아서 폴리시(cancelCoupon) 처리하였다. (getOrderId()를 호출하여 Correlation-key 연결)

coupon 서비스의 PolicyHandler.java
```
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

    }
    ...생략
```


- Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가?

쿠폰(coupon)서비스의 포트 추가(기존:8085, 추가:8095)하여 2개의 노드로 배송서비스를 실행한다. 

bookdelivery topic의 partition은 1개이기 때문에 새로 구동시킨 8095 포트의 서비스만 partition이 할당된다.

![쿠폰-8085포트](https://user-images.githubusercontent.com/85722733/126621466-2ab00ca0-9836-4ca6-8333-6bd93ded32ec.png)

![쿠폰-8095포트](https://user-images.githubusercontent.com/85722733/126621479-7e64110f-21c6-4b15-ad61-c887735c6468.png)


결제(payment)에서 결제취소 이벤트가 발생하면 8095포트에 있는 coupon 서비스에게만 이벤트 메세지가 수신되게 되고

8085포트의 coupon 서비스의 경우 메세지를 수신받지 못한다.

![쿠폰-8095에서쿠폰취소로직](https://user-images.githubusercontent.com/85722733/126617867-b044ed1e-a1b9-4146-8d14-e8c855c42705.png)

8095 포트를 중지 시키면 8085포트의 coupon 서비스에서 partition을 할당받는다
![쿠폰-8085포트로재할당](https://user-images.githubusercontent.com/85722733/126617939-c650cab4-e74a-4784-9c01-a7268d1700cc.png)


### SAGA 패턴
- 취소에 따른 보상 트랜잭션을 설계하였는가(Saga Pattern)

SAGA 패턴은 각 서비스의 트랜잭션 완료 후에 다음 서비스가 트리거 되어 트랜잭션을 실행하는 방법으로

현재 BookDelivery 시스템도 SAGA 패턴으로 설계되어 있다.

#### SAGA 패턴에 맞춘 트랜잭션 실행

![사가1_쿠폰](https://user-images.githubusercontent.com/85722733/126601428-e39fc7b8-6d42-4532-a92e-676429d0259e.png)

order 서비스의 주문 생성이 완료되면 payment 서비스를 트리거하게 되고 결제를 발생시킨다

실행한 결과는 아래와 같다

주문 생성 시 sync 호출로 인해 결제가 발생하여 결제 승인이 나게 되며, 

![주문생성api](https://user-images.githubusercontent.com/85722733/126621651-bc1b256b-e5b1-4488-8f45-d912ec4f8859.png)

![주문생성-결제생성](https://user-images.githubusercontent.com/85722733/126621687-fbef2a91-8fea-4776-95dd-ed986814df28.png)

![주문생성-카프카](https://user-images.githubusercontent.com/85722733/126621707-37066a60-2b99-40de-b666-23671a417b53.png)

이를 ordermanagement 서비스에서 연계받아 주문내역을 수신받게 된다

![주문생성-mgmt주문내역전달](https://user-images.githubusercontent.com/85722733/126621715-84c89d90-666b-4ec2-bb53-d51912393f56.png)

점주가 주문을 접수하여 주문접수 건이 생성되고, 이후 점주가 쿠폰 발행을 위해 couponStatus를 "sending"으로 coupon 서비스를 트리거하면 쿠폰 발행이 되며

![주문접수생성api](https://user-images.githubusercontent.com/85722733/126621800-6b91c94d-717d-46ff-ac7b-b7b8d6149e2a.png)

![주문접수생성-쿠폰생성](https://user-images.githubusercontent.com/85722733/126621945-171b2a77-ba09-44fb-9fa5-20240f908874.png)

이와 동시에 delivery 서비스에서 배송시작 이벤트가 트리거 된다

![주문접수생성-startDelivery](https://user-images.githubusercontent.com/85722733/126621868-bf7786b0-4cae-4b65-8990-b23642434fe6.png)

![주문접수생성-배송생성](https://user-images.githubusercontent.com/85722733/126621934-648abea3-bf9c-4679-84b9-17e52b0e9220.png)

![주문접수생성-카프카](https://user-images.githubusercontent.com/85722733/126621960-a1673a48-b307-4f6c-9877-078d35c6010c.png)


#### SAGA 패턴에 맞춘 Roll-Back 
![사가2_쿠폰](https://user-images.githubusercontent.com/85722733/126597194-9fbfc4ae-9d46-4dae-97ab-0dd139ce7155.png)

order 서비스에서 주문취소가 발생하면 발행된 이벤트가 ordermanagement 서비스, delivery 서비스, payment 서비스, coupon 서비스로 트리거되어 해당 주문에 대해 주문접수취소, 배송취소, 결제취소 및 쿠폰취소가 되도록 보상 트랜잭션을 발생시킨다

실행한 결과는 아래와 같다

고객의 주문취소로 인하여 주문 상태를 주문취소로 업데이트 시 

![주문취소api](https://user-images.githubusercontent.com/85722733/126622578-5ca4265f-aaed-4c6a-9681-c7318216a59e.png)

OrderCanceled 이벤트로 인하여 orderManagement 서비스에서 주문상태가 주문접수취소로 업데이트되어 이벤트가 발생되고

![주문취소-주문접수취소](https://user-images.githubusercontent.com/85722733/126622609-0c9a40b6-7817-414d-85aa-3e84d3cffdbc.png)

이로 인해 트리거되어 delivery 및 payment 서비스에서도 취소 이벤트가 발생하게 되는데

![주문취소-배송취소](https://user-images.githubusercontent.com/85722733/126622624-beaa08a8-811a-4856-b6a1-fb0e9dfc0a52.png)

![주문취소-결제취소](https://user-images.githubusercontent.com/85722733/126622641-74b0fac7-042b-4383-88ed-b1e5f0b24110.png)

payment 서비스에서 결제취소 이벤트 발행 시 coupon 서비스에서 subscribe하여 해당 주문 건으로 고객에게 발행된 쿠폰에 대한 상태를 invalid로 변경하면서 쿠폰을 무효화하게 된다 

![주문취소-쿠폰취소폴리시호출](https://user-images.githubusercontent.com/85722733/126622650-83a0b5d2-1112-49d0-b0bb-bcd114bd0312.png)

![주문취소-쿠폰취소](https://user-images.githubusercontent.com/85722733/126624283-715664a9-16af-42bb-b5ad-71a9ef1f2a08.png)

![주문취소-카프카](https://user-images.githubusercontent.com/85722733/126622677-4c8f9cbe-0b8b-41c8-bc88-4e99e937c1ff.png)


### CQRS
- CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?

고객이 본인에게 발행된 쿠폰을 마이쿠폰페이지에서 확인할 수 있어야 한다는 요구사항에 따라 주문 서비스 내에 MyCoupon View를 모델링하였다

![마이쿠폰](https://user-images.githubusercontent.com/85722733/126601519-2fb8f8d1-5913-4784-b3d6-2188df629f7a.png)

고객에게 쿠폰 발행(CouponSent) 시 MyCoupon 데이터가 "valid" 상태로 생성되며 

쿠폰무효화(취소)(CouponCanceled) 이벤트에 따라 쿠폰상태가 "invalid"로 업데이트되도록 모델링하였다

MyCoupon View 의 속성값

![마이쿠폰속성](https://user-images.githubusercontent.com/85722733/126601545-9c569840-35d3-4a89-a637-bf9449bcf2d7.png)

MSAEz 모델링 도구 내 View CQRS 설정 샘플

![마이쿠폰로직](https://user-images.githubusercontent.com/85722733/126601575-5018829b-b4a9-408d-9ddc-dc75aa91e053.png)

자동생성된 소스는 아래와 같다

MyCoupon CQRS처리를 위해 주문, 결제, 주문관리, 배송, 쿠폰 서비스와 별개로 조회를 위한 MyCoupon_table 테이블이 생성된다

MyCoupon.java : 엔티티 클래스
```
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
```
MyCouponRepository.java : 퍼시스턴스
```
package bookdelivery;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface MyCouponRepository extends CrudRepository<MyCoupon, Long> {

    Optional<MyCoupon> findByCouponCode(Long couponCode);

}
```
MyCouponViewHandler.java : 아래와 같이 쿠폰발행을 통한 MyCoupon 데이터 생성 및 쿠폰취소에 따른 쿠폰상태 변경에 대한 이벤트 수신 처리부가 있다

쿠폰 발행 시 MyCoupon 데이터 생성 이벤트
```
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
```
쿠폰 취소 시 생성되어있는 MyCoupon 데이터의 쿠폰상태 업데이트 이벤트
```
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
```

CQRS 테스트

ordermanagement 서비스에서 주문접수 건 생성 시 쿠폰이 정상 발행됨을 확인

![주문접수생성api](https://user-images.githubusercontent.com/85722733/126618242-e1449cb4-c94e-4b3d-8d14-c3c2fc6cd195.png)

![주문접수생성-쿠폰생성](https://user-images.githubusercontent.com/85722733/126618304-0bf8f62a-fc35-4618-80e5-35dc2fdfa4cb.png)

아래와 같이 MyCoupon에 쿠폰상태가 "valid"로 정상 등록되어 조회됨을 확인

![주문접수생성-마이쿠폰생성](https://user-images.githubusercontent.com/85722733/126618162-4c36b7bf-342f-4461-bd62-4bea267626e0.png)

주문접수취소에 따른 결제취소완료 시 쿠폰취소 이벤트가 발행되어 쿠폰상태가 "invalid"로 변경되며 MyCoupon에 해당 쿠폰에 대한 쿠폰상태가 "invalid"로 동일하게 조회된다

![주문취소api](https://user-images.githubusercontent.com/85722733/126618363-00c747b3-6b74-4ba8-a438-9165a0ba91d3.png)

![주문취소-주문접수취소](https://user-images.githubusercontent.com/85722733/126618468-c0b17305-5010-44fd-bedf-3ee3b301c492.png)

![주문취소-결제취소](https://user-images.githubusercontent.com/85722733/126618484-06fcf826-8d0e-4b94-b98e-c56085c12bbe.png)

![주문취소-쿠폰취소폴리시호출](https://user-images.githubusercontent.com/85722733/126618493-f5a64707-5ebe-47ee-9808-9128c332a916.png)

![주문취소-카프카](https://user-images.githubusercontent.com/85722733/126618508-0c90460a-d94f-4098-bcdc-b6e974fade32.png)

![주문취소-쿠폰취소](https://user-images.githubusercontent.com/85722733/126618593-0e804dc6-41bf-4d68-b8ab-de0ac886dea6.png)


- Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?

ordermanagement 서비스만 구동되고 delivery 서비스는 멈춰있는 상태이다. 주문관리에 이벤트가 발생하면 카프카 큐에 정상적으로 들어감을 확인할 수 있다.

![배송중단-주문접수api성공](https://user-images.githubusercontent.com/85722733/126618714-cf610a8d-12c8-4954-8bff-c34fc0bd4f2c.png)

카프카 Consumer 캡쳐
![배송중단-주문접수성공](https://user-images.githubusercontent.com/85722733/126618757-ded9e821-22f5-44fd-a6ac-768d25c3377c.png)


배송(delivery)서비스 실행 및 실행 후 카프카에 적재된 메세지 수신 확인
```
cd delivery
mvn spring-boot:run
```
![배송재시작-로그](https://user-images.githubusercontent.com/85722733/126618891-dc7af601-7cda-4784-b914-1730ac4c7254.png)

카프카 Consumer 캡쳐
![배송재시작-카프카](https://user-images.githubusercontent.com/85722733/126618912-f347d685-3f71-481f-81b4-b768a6bae05d.png)


## 폴리글랏 퍼시스턴스

배송 서비스(delivery)는 실시간 배송위치 추적 등 추후 지도(GIS) 기반 서비스의 확장까지 고려하여, 공간(Spatial)부분에 상당한 강점이 있는 데이터베이스인 postgreSQL로 선정하여

자동생성된 DB설정인 H2에서 postgreSQL로 변경하였다

먼저, AWS RDS를 통하여 postgreSQL을 프리티어로 생성하였다

AWS > RDS > 데이터베이스 생성

![폴리글랏-RDS0](https://user-images.githubusercontent.com/85722733/126958629-d2b82388-b2a2-4779-bada-e3ef6096fc7f.png)

생성된 모습

![폴리글랏-RDS1](https://user-images.githubusercontent.com/85722733/126958785-e76d3145-dcb2-4485-ba58-fa597d51970f.png)

접속 허용을 위해 보안그룹을 추가하고, 인바운드 규칙에 모든 TCP를 허용하였다

![폴리글랏-RDS2](https://user-images.githubusercontent.com/85722733/126958811-3431de6f-d7ee-4b86-b24e-4b3902e4d0eb.png)

로컬PC에서 PgAdmin을 통해 해당 DB에 접속가능함을 확인하고

![폴리글랏_PGADMIN_1](https://user-images.githubusercontent.com/85722733/126959117-616675b9-40f2-4fdc-8863-299cfeaf3399.png)

소스에서는 delivery 서비스의 pom.xml 의존성을 변경해 주고

기존 h2 → postgreSQL 변경 

![폴리글랏-변경설정2](https://user-images.githubusercontent.com/85722733/126959584-7de9cf61-8e9f-4c9e-aefe-d6ec4627edbc.png)

delivery 서비스의 application.yml 파일에 dababase 속성도 변경해 주었다

![폴리글랏-변경설정1](https://user-images.githubusercontent.com/85722733/126960720-b1ba9907-5a97-484a-ab92-a835f468af37.png)

![폴리글랏-변경설정11](https://user-images.githubusercontent.com/85722733/126979443-03090b26-499e-4bc8-a062-311fa92b7425.png)

이후 로컬에서 delivery 서비스를 mvn spring-boot:run 으로 구동한 결과 

PgAdmin을 통해 배송서비스(delivery) 관련 테이블(delivery_table)이 postgres에 생성된 모습을 확인하였다

![폴리글랏-결과](https://user-images.githubusercontent.com/85722733/126959910-ed04b5d3-019e-4c90-b27f-ae09d48a1d71.png)

운영에도 배포한 결과 delivery 서비스가 변경된 postgreSQL DB로 정상적으로 동작함을 확인하였다

![폴리글랏-운영](https://user-images.githubusercontent.com/85722733/126979775-257f277e-0d7d-4705-8998-7874e9a11874.png)


## API 게이트웨이

- API GW를 통하여 마이크로 서비스들의 진입점을 통일할 수 있는가?

아래는 MSAEZ를 통해 자동 생성된 gateway 서비스의 application.yml이며, 마이크로서비스들의 진입점을 통일하여 URL Path에 따라서 마이크로서비스별 서로 다른 포트로 라우팅시키도록 설정되었다

gateway 서비스의 application.yml 파일 

```
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: order
          uri: http://localhost:8081
          predicates:
            - Path=/orders/**, /myPages/**, /myCoupons/**
        - id: ordermanagement
          uri: http://localhost:8082
          predicates:
            - Path=/ordermgmts/** 
        - id: delivery
          uri: http://localhost:8083
          predicates:
            - Path=/deliveries/** 
        - id: payment
          uri: http://localhost:8084
          predicates:
            - Path=/payments/** 
        - id: coupon
          uri: http://localhost:8085
          predicates:
            - Path=/coupons/**            
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

---
```

Gateway 포트인 8088을 통해서 발행된 쿠폰이 정상 조회되는 것을 확인

![주문접수생성-쿠폰생성](https://user-images.githubusercontent.com/85722733/126625049-22fbc909-2bc7-421c-95a8-7e0bccc8a229.png)


# 운영
## Deploy/Pipeline

**yaml을 이용한 배포**

배포에 사용한 Deployment.yaml

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: coupon
  namespace: bookdelivery
  labels:
    app: coupon
spec:
  replicas: 1
  selector:
    matchLabels:
      app: coupon
  template:
    metadata:
      labels:
        app: coupon
    spec:
      containers:
      - name: coupon
        image: 879772956301.dkr.ecr.ca-central-1.amazonaws.com/user23-coupon:latest
        ports:
        - containerPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: coupon
  namespace: bookdelivery       
  labels:
    app: coupon
spec:
  type: ClusterIP
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: coupon
```

![yaml파일배포](https://user-images.githubusercontent.com/85722733/126923436-781c8e3a-e936-4f34-a04f-6e6d53ea1d49.png)

![yaml파일배포결과](https://user-images.githubusercontent.com/85722733/126923460-a85f9bf2-d858-4b2f-b722-edf487e60a1d.png)


## 동기식 호출 / Circuit Breaker / 장애격리

서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현 

점주의 주문관리 접수 시 쿠폰발행 요청(ordermanagement → coupon)이 과도할 경우 서킷 브레이커를 통한 회로 차단을 통해 장애 격리를 하려고 한다

Hystrix 를 설정: 주문관리 요청처리 쓰레드에서 처리시간이 610 ms가 넘어서기 시작하여 어느정도 유지되면 쿠폰서비스가 차단되도록 (요청을 빠르게 실패처리) 설정  

![서킷-0](https://user-images.githubusercontent.com/85722733/126925278-9ee5e353-85a6-4577-bdc0-67e40cf8c7b9.png)

쿠폰 서비스의 @PrePersist를 통한 부하 처리 - 400 ms에서 증감 220 ms 정도 수준으로 설정  

![서킷-1](https://user-images.githubusercontent.com/85722733/126925099-703fa020-8819-4204-8b82-08d7b45930b3.png)

부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인: 동시사용자 100명 60초 동안 실시  
```
root@siege:/# siege -c100 -t60S -v --content-type "application/json" 'http://10.100.37.173:8080/ordermgmts POST {"orderId": "1", "itemName": "ITbook", "qty": "3", "customerName": "HeidiCho", "customerId": "7777", "deliveryAddress": "kyungkido sungnamsi", "deliveryPhoneNumber": "01012341234", "orderStatus": "orderTaken"}'
```

요청 상태에 따라 회로 열기/닫기가 반복되는 모습 확인

![서킷-2](https://user-images.githubusercontent.com/85722733/126925425-7348b6a5-d896-4a6a-8893-72d0783a88cf.png)

siege 테스트 결과 연결시도 대비 성공률이 약 73% 로서 서킷 브레이커가 정상 동작함을 확인하였다

![서킷-3](https://user-images.githubusercontent.com/85722733/126925404-da44f085-d8f0-4979-a7b3-235f8faa9973.png)


## Autoscale (HPA)

쿠폰 서비스에 HPA를 설정한다. 평균대비 CPU 15% 초과시 3개까지 pod 추가  

![오토스케일세팅](https://user-images.githubusercontent.com/85722733/126853232-1dd187f9-5915-4635-8b94-3f4d05013a1e.png)

현재 쿠폰서비스 pod 상태 확인  

![hpa_초기pod](https://user-images.githubusercontent.com/85722733/126853240-bdd36362-bb73-412a-9184-c0f48d5452c0.png)

siege 로 부하테스트를 진행  
```
root@siege:/# siege -c100 -t60S -v --content-type "application/json" 'http://10.100.37.173:8080/coupons'
```

아래와 같이 쿠폰 pod가 3개까지 증가된 것을 확인할 수 있다

![hpa_시즈후기](https://user-images.githubusercontent.com/85722733/126853292-e58003bb-1622-442e-9e48-d20f2b6da283.png)

siege를 통한 부하가 중단이 되고 시간이 흐른 후 CPU가 감소하여 쿠폰서비스 pod가 다시 1개로 줄어든 것을 확인하였다

![hpa부하중단후기](https://user-images.githubusercontent.com/85722733/126853401-46941b01-443a-416c-bf96-b0e5856fe30d.png)


## Zero-downtime deploy (Readiness Probe)
(무정지 배포)

서비스의 무정지 배포를 위하여 쿠폰(coupon) 서비스의 배포 yaml 파일에 readinessProbe 옵션을 추가하였다

Deployment.yaml
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: coupon
  namespace: bookdelivery
  labels:
    app: coupon
spec:
  replicas: 1
  selector:
    matchLabels:
      app: coupon
  template:
    metadata:
      labels:
        app: coupon
    spec:
      containers:
      - name: coupon
        image: 879772956301.dkr.ecr.ca-central-1.amazonaws.com/user23-coupon:latest
        ports:
        - containerPort: 8080
        readinessProbe:
          httpGet:
            path: '/coupons'
            port: 8080
          initialDelaySeconds: 10
          timeoutSeconds: 2
          periodSeconds: 5
          failureThreshold: 2  
---
apiVersion: v1
kind: Service
metadata:
  name: coupon
  namespace: bookdelivery       
  labels:
    app: coupon
spec:
  type: ClusterIP
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: coupon

```
배포 수행

![레디니스-1](https://user-images.githubusercontent.com/85722733/126932315-d0c3a8fb-08b8-488a-9c8b-4ee74895409e.png)

siege 를 통해 100명의 가상의 유저가 60초동안 쿠폰 서비스를 지속적으로 호출하게 함과 동시에
```
root@siege:/# siege -c100 -t60S -v --content-type "application/json" 'http://10.100.37.173:8080/coupons'
```
kubectl set image 명령어를 통해 배포를 수행하였다.

![레디니스0](https://user-images.githubusercontent.com/85722733/126932382-d4630594-2e2e-448d-8138-d5086b8ca570.png)

siege 테스트 결과 연결시도 대비 성공률이 100% 로서 readinessProbe 옵션을 통해 무정지 배포를 확인하였다.

![레디니스-3](https://user-images.githubusercontent.com/85722733/126932402-176d3367-7778-4459-85cb-6f81102774e3.png)


## ConfigMap
ConfigMap은 컨테이너 이미지로부터 설정 정보를 분리할 수 있도록 Kubernetes에서 제공해주는 설정인데,

환경변수나 설정값 들을 환경변수로 관리해 Pod가 생성될 때 이 값을 주입할 수 있다 

bookdelivery 시스템에서는 NAMESPACENAME 값을 저장하여 사용하기 위해서 아래와 같이 bookdelivery-config라는 이름의 configmap 에 nsname의 값을 'bookdelivery'로 저장했다

컨피그맵 생성 및 확인

![2_컨피그맵생성](https://user-images.githubusercontent.com/85722733/126934568-60237499-9264-447f-84bd-4fc73a1be89a.png)

쿠폰서비스 배포 yaml에 아래와 같이 NAMESPACENAME라는 환경 변수에 위 configmap에서 정의한 nsname의 값을 설정한다

Deployment_cm.yaml
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: coupon
  namespace: bookdelivery
  labels:
    app: coupon
spec:
  replicas: 1
  selector:
    matchLabels:
      app: coupon
  template:
    metadata:
      labels:
        app: coupon
    spec:
      containers:
      - name: coupon
        image: 879772956301.dkr.ecr.ca-central-1.amazonaws.com/user23-coupon:latest
        ports:
        - containerPort: 8080
        env:
          - name: NAMESPACENAME
            valueFrom:
              configMapKeyRef:
                name: bookdelivery-config
                key: nsname
...생략
```

배포 후 쿠폰 pod 생성 확인
```
]root@labs-565305537:/home/project# kubectl apply -f Deployment_cm.yaml
deployment.apps/coupon created
service/coupon created
```

![2_컨피그맵_pod생성](https://user-images.githubusercontent.com/85722733/126934697-d8d1415d-9c2a-4c1c-a8f9-cc32a25d77e5.png)

아래 명령어를 통해 해당 쿠폰 pod로 진입하여 환경변수 및 echo로 NAMESPACENAME 값을 확인한다

```
]root@labs-565305537:/home/project# kubectl exec -it pod/coupon-5954668d55-gxmnd -n bookdelivery -- /bin/sh
```

![컨피그맵결과1](https://user-images.githubusercontent.com/85722733/126934819-d1e5978d-4664-4bc9-9b2c-c57ae4d4ad7e.png)

![컨피그맵결과2](https://user-images.githubusercontent.com/85722733/126934884-1a432808-c1a4-4689-9558-f4fbfb80638e.png)

configmap을 통하여 nsname 값으로 저장한 'bookdelivery'가 해당 pod의 NAMESPACENAME 환경변수값으로 정상 주입됨을 확인하였다


## Self-healing (Liveness Probe)

쿠폰(coupon) 서비스의 배포 yaml 파일에 Pod 내 /tmp/healthy 파일을 5초마다 체크하도록 livenessProbe 옵션을 추가하였다

```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: coupon
  namespace: bookdelivery
  labels:
    app: coupon
spec:
  replicas: 1
  selector:
    matchLabels:
      app: coupon
  template:
    metadata:
      labels:
        app: coupon
    spec:
      containers:
      - name: coupon
        image: 405574919273.dkr.ecr.us-east-2.amazonaws.com/csa-coupon:latest
        ports:
        - containerPort: 8080
        livenessProbe:
          exec:
            command:
            - cat 
            - /tmp/healthy
          initialDelaySeconds: 15
          periodSeconds: 5
```
yaml 파일을 실행하여 쿠폰 pod 가 생성되었다
```
tjddk0114@SKTP038564PN003:~$ kubectl create -f test_liveness.yaml
deployment.apps/coupon created
```

Pod 구동 시 Running 상태이나 Pod 내 체크 대상인 /tmp/healthy 파일이 없기 때문에 livenessProbe 옵션의 "Self-healing" 특징 대로 계속 Retry하여 Restart 된 것이 확인된다

![liveness-restart](https://user-images.githubusercontent.com/85722733/126854835-41c8b358-022a-4438-b87e-51e94053988e.png)

kubectl describe 명령어로 쿠폰 Pod 상태 확인 시 livenessProbe 관련 실패 로그

![liveness-로그](https://user-images.githubusercontent.com/85722733/126854895-bf6dac1b-c34f-4141-8daf-30ae790ea941.png)

쿠폰 Pod 내부로 진입하여 touch 명령어를 통해 /tmp/healthy 파일 생성 시 Restart가 2번째에서 중단되고 Pod가 정상 동작함을 확인하였다 (1회 Fail 후 파일 생성되어 2번째에 성공)

```
tjddk0114@SKTP038564PN003:~$ kubectl exec -it pod/coupon-5d5896d74-d829p -n bookdelivery -- /bin/sh
/ # touch /tmp/healthy
```

![liveness-결과](https://user-images.githubusercontent.com/85722733/126854781-e9fd2a13-e0ea-49bc-bfe1-98d97aec3ab6.png)
