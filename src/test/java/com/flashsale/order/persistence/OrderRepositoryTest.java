
package com.flashsale.order.persistence;

import com.flashsale.catalog.domain.Product;
import com.flashsale.catalog.persistence.ProductRepository;
import com.flashsale.campaign.domain.Campaign;
import com.flashsale.campaign.persistence.CampaignRepository;
import com.flashsale.identity.domain.User;
import com.flashsale.identity.persistence.UserRepository;
import com.flashsale.inventory.domain.Inventory;
import com.flashsale.inventory.persistence.InventoryRepository;
import com.flashsale.order.domain.Order;
import com.flashsale.order.domain.OrderStatus;
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
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

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
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveAndFindOrderByReservationId() {

        Long userId = createTestUser();
        Long reservationId = createTestReservation(userId);

        Order order = Order.create(
                userId,
                reservationId,
                new BigDecimal("99.98")
        );

        Order savedOrder = orderRepository.save(order);

        var result = orderRepository.findByReservationId(reservationId);

        assertThat(result).isPresent();
        assertThat(savedOrder.getId()).isNotNull();
        assertThat(result.get().getUserId()).isEqualTo(userId);
        assertThat(result.get().getReservationId()).isEqualTo(reservationId);
        assertThat(result.get().getTotalAmount())
                .isEqualByComparingTo("99.98");
        assertThat(result.get().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.get().getCreatedAt()).isNotNull();
        assertThat(result.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindOrdersByUserId() {

        Long userId = createTestUser();
        Long reservationId = createTestReservation(userId);

        Order order = Order.create(
                userId,
                reservationId,
                new BigDecimal("99.98")
        );

        orderRepository.save(order);

        List<Order> orders = orderRepository.findByUserId(userId);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    void shouldFindOrdersByStatus() {

        Long userId = createTestUser();
        Long reservationId = createTestReservation(userId);

        Order order = Order.create(
                userId,
                reservationId,
                new BigDecimal("99.98")
        );

        orderRepository.save(order);

        List<Order> orders =
                orderRepository.findByStatus(OrderStatus.PENDING);

        assertThat(orders)
                .extracting(Order::getStatus)
                .contains(OrderStatus.PENDING);
    }

    private Long createTestUser() {

        User user = User.create(
                "test-" + UUID.randomUUID() + "@example.com",
                "test-password-hash"
        );

        return userRepository.save(user).getId();
    }

    private Long createTestReservation(Long userId) {

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
                userId,
                campaign.getId(),
                product.getId(),
                2,
                new BigDecimal("49.99")
        );

        return reservationRepository.save(reservation).getId();
    }
}
