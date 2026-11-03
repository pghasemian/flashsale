package com.flashsale.reservation.persistence;

import com.flashsale.catalog.domain.Product;
import com.flashsale.catalog.persistence.ProductRepository;
import com.flashsale.campaign.domain.Campaign;
import com.flashsale.campaign.persistence.CampaignRepository;
import com.flashsale.identity.domain.User;
import com.flashsale.identity.persistence.UserRepository;
import com.flashsale.inventory.domain.Inventory;
import com.flashsale.inventory.persistence.InventoryRepository;
import com.flashsale.reservation.domain.Reservation;
import com.flashsale.reservation.domain.ReservationStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveAndFindReservationsByUserId() {

        Long userId = createTestUser();

        Long[] inventoryIds = createTestInventory();

        Long campaignId = inventoryIds[0];
        Long productId = inventoryIds[1];

        Reservation reservation = Reservation.create(
                userId,
                campaignId,
                productId,
                2,
                new BigDecimal("49.99")
        );

        Reservation savedReservation =
                reservationRepository.save(reservation);

        List<Reservation> reservations =
                reservationRepository.findByUserId(userId);

        assertThat(savedReservation.getId()).isNotNull();
        assertThat(reservations).hasSize(1);

        Reservation result = reservations.get(0);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getCampaignId()).isEqualTo(campaignId);
        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getQuantity()).isEqualTo(2);
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(result.getUnitPrice())
                .isEqualByComparingTo("49.99");
        assertThat(result.getExpiresAt()).isAfter(Instant.now());
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindReservationsByStatus() {

        Long userId = createTestUser();

        Long[] inventoryIds = createTestInventory();

        Reservation reservation = Reservation.create(
                userId,
                inventoryIds[0],
                inventoryIds[1],
                1,
                new BigDecimal("25.00")
        );

        reservationRepository.save(reservation);

        List<Reservation> activeReservations =
                reservationRepository.findByStatus(ReservationStatus.ACTIVE);

        assertThat(activeReservations)
                .extracting(Reservation::getStatus)
                .contains(ReservationStatus.ACTIVE);
    }

    private Long createTestUser() {

        User user = User.create(
                "test-" + java.util.UUID.randomUUID() + "@example.com",
                "test-password-hash"
        );

        return userRepository.save(user).getId();
    }

    private Long[] createTestInventory() {

        Product product = Product.create(
                "Test Product",
                "Test Description",
                "SKU-" + java.util.UUID.randomUUID(),
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

        return new Long[]{
                campaign.getId(),
                product.getId()
        };
    }
}