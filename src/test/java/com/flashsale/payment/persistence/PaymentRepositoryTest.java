
package com.flashsale.payment.persistence;

import com.flashsale.catalog.domain.Product;
import com.flashsale.catalog.persistence.ProductRepository;
import com.flashsale.campaign.domain.Campaign;
import com.flashsale.campaign.persistence.CampaignRepository;
import com.flashsale.identity.domain.User;
import com.flashsale.identity.persistence.UserRepository;
import com.flashsale.inventory.domain.Inventory;
import com.flashsale.inventory.persistence.InventoryRepository;
import com.flashsale.order.domain.Order;
import com.flashsale.order.persistence.OrderRepository;
import com.flashsale.payment.domain.Payment;
import com.flashsale.payment.domain.PaymentStatus;
import com.flashsale.reservation.domain.Reservation;
import com.flashsale.reservation.persistence.ReservationRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveAndFindPaymentByOrderId() {

        Long orderId = createTestOrder();

        Payment payment = Payment.create(
                orderId,
                new BigDecimal("99.98"),
                UUID.randomUUID().toString()
        );

        Payment savedPayment = paymentRepository.save(payment);

        List<Payment> payments =
                paymentRepository.findByOrderId(orderId);

        assertThat(savedPayment.getId()).isNotNull();
        assertThat(payments).hasSize(1);

        Payment result = payments.get(0);

        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getAmount())
                .isEqualByComparingTo("99.98");
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getIdempotencyKey()).isNotBlank();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindPaymentByIdempotencyKey() {

        Long orderId = createTestOrder();

        String idempotencyKey = UUID.randomUUID().toString();

        Payment payment = Payment.create(
                orderId,
                new BigDecimal("99.98"),
                idempotencyKey
        );

        paymentRepository.save(payment);

        var result =
                paymentRepository.findByIdempotencyKey(idempotencyKey);

        assertThat(result).isPresent();
        assertThat(result.get().getOrderId()).isEqualTo(orderId);
        assertThat(result.get().getIdempotencyKey())
                .isEqualTo(idempotencyKey);
    }

    @Test
    void shouldFindPaymentsByStatus() {

        Long orderId = createTestOrder();

        Payment payment = Payment.create(
                orderId,
                new BigDecimal("99.98"),
                UUID.randomUUID().toString()
        );

        paymentRepository.save(payment);

        List<Payment> payments =
                paymentRepository.findByStatus(PaymentStatus.PENDING);

        assertThat(payments)
                .extracting(Payment::getStatus)
                .contains(PaymentStatus.PENDING);
    }

    private Long createTestOrder() {

        User user = User.create(
                "test-" + UUID.randomUUID() + "@example.com",
                "test-password-hash"
        );

        user = userRepository.save(user);

        Product product = Product.create(
                "Test Product",
                "Test Description",
                "SKU-" + UUID.randomUUID(),
                new BigDecimal("100.00")
        );

        product = productRepository.save(product);

        Instant startTime = Instant.now().plusSeconds(3600);

        Campaign campaign = Campaign.create(
                "Test Campaign",
                "Test Description",
                startTime,
                startTime.plusSeconds(86400)
        );

        campaign = campaignRepository.save(campaign);

        jdbcTemplate.update(
                """
                INSERT INTO campaign_products
                    (campaign_id, product_id, sale_price)
                VALUES (?, ?, ?)
                """,
                campaign.getId(),
                product.getId(),
                new BigDecimal("49.99")
        );

        Inventory inventory = Inventory.create(
                campaign.getId(),
                product.getId(),
                100
        );

        inventoryRepository.save(inventory);

        Reservation reservation = Reservation.create(
                user.getId(),
                campaign.getId(),
                product.getId(),
                2,
                new BigDecimal("49.99")
        );

        reservation = reservationRepository.save(reservation);

        Order order = Order.create(
                user.getId(),
                reservation.getId(),
                new BigDecimal("99.98")
        );

        return orderRepository.save(order).getId();
    }
}
