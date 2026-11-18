package com.flashsale.reservation.integration;

import com.flashsale.FlashsaleApplication;
import com.flashsale.dto.reservation.CreateReservationRequest;
import com.flashsale.reservation.application.ReservationExpiryWorker;
import com.flashsale.reservation.application.ReservationService;
import com.flashsale.reservation.persistence.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = FlashsaleApplication.class)
class ReservationIdempotencyIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReservationExpiryWorker reservationExpiryWorker;

    private Long createTestUser() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO users (email, password_hash, enabled)
                VALUES ('idempotency-' || gen_random_uuid() || '@example.com', 'test-hash', true)
                RETURNING id
                """, Long.class);
    }

    private Long createTestProduct() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO products (name, sku, price, active)
                VALUES ('Test Product', 'IDEMPOTENCY-' || gen_random_uuid(), 100.00, true)
                RETURNING id
                """, Long.class);
    }

    private Long createTestCampaign() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO campaigns (
                    name, description, start_time, end_time, status
                )
                VALUES (
                    'Test Campaign',
                    'Integration test campaign',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP + INTERVAL '1 day',
                    'ACTIVE'
                )
                RETURNING id
                """, Long.class);
    }

    private void createCampaignProduct(Long campaignId, Long productId) {
        jdbcTemplate.update("""
                INSERT INTO campaign_products (campaign_id, product_id, sale_price)
                VALUES (?, ?, 80.00)
                """, campaignId, productId);
    }

    private void createInventory(Long campaignId, Long productId) {
        jdbcTemplate.update("""
                INSERT INTO inventory (
                    campaign_id, product_id, total_stock, reserved_stock
                )
                VALUES (?, ?, 10, 0)
                """, campaignId, productId);
    }

    @Test
    void shouldCreateOnlyOneReservationForConcurrentRequestsWithSameIdempotencyKey()
            throws Exception {

        Long userId = createTestUser();
        Long productId = createTestProduct();
        Long campaignId = createTestCampaign();

        createCampaignProduct(campaignId, productId);
        createInventory(campaignId, productId);

        CreateReservationRequest request =
                new CreateReservationRequest(campaignId, productId, 2);

        String idempotencyKey = UUID.randomUUID().toString();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<?> first = executor.submit(() -> {
                ready.countDown();
                start.await();
                return reservationService.createReservation(
                        userId, request, idempotencyKey);
            });

            Future<?> second = executor.submit(() -> {
                ready.countDown();
                start.await();
                return reservationService.createReservation(
                        userId, request, idempotencyKey);
            });

            ready.await();
            start.countDown();

            first.get();
            second.get();

            Integer reservationCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM reservations WHERE idempotency_key = ?",
                    Integer.class,
                    idempotencyKey
            );

            Integer reservedStock = jdbcTemplate.queryForObject("""
                            SELECT reserved_stock
                            FROM inventory
                            WHERE campaign_id = ? AND product_id = ?
                            """,
                    Integer.class,
                    campaignId,
                    productId
            );

            assertThat(reservationCount).isEqualTo(1);
            assertThat(reservedStock).isEqualTo(2);

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldRejectSameIdempotencyKeyWithDifferentPayload() {

        Long userId = createTestUser();
        Long productId = createTestProduct();
        Long campaignId = createTestCampaign();

        createCampaignProduct(campaignId, productId);
        createInventory(campaignId, productId);

        String idempotencyKey = UUID.randomUUID().toString();

        CreateReservationRequest firstRequest =
                new CreateReservationRequest(campaignId, productId, 2);

        CreateReservationRequest differentRequest =
                new CreateReservationRequest(campaignId, productId, 3);

        reservationService.createReservation(
                userId, firstRequest, idempotencyKey);

        assertThatThrownBy(() ->
                reservationService.createReservation(
                        userId, differentRequest, idempotencyKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Idempotency key was already used with a different request");
    }

    @Test
    void shouldNeverReserveMoreThanAvailableStockUnderConcurrentRequests()
            throws Exception {

        Long userId = createTestUser();
        Long productId = createTestProduct();
        Long campaignId = createTestCampaign();

        createCampaignProduct(campaignId, productId);
        createInventory(campaignId, productId);

        int requestCount = 100;
        int requestedQuantity = 1;

        AtomicInteger successfulReservations = new AtomicInteger();
        AtomicInteger rejectedReservations = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch ready = new CountDownLatch(20);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < requestCount; i++) {
                String idempotencyKey = UUID.randomUUID().toString();

                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();

                    try {
                        CreateReservationRequest request =
                                new CreateReservationRequest(
                                        campaignId,
                                        productId,
                                        requestedQuantity
                                );

                        reservationService.createReservation(
                                userId,
                                request,
                                idempotencyKey
                        );

                        successfulReservations.incrementAndGet();

                    } catch (IllegalStateException exception) {
                        if (!"Insufficient stock".equals(exception.getMessage())) {
                            throw exception;
                        }

                        rejectedReservations.incrementAndGet();
                    }

                    return null;
                }));
            }

            ready.await();
            start.countDown();

            for (Future<?> future : futures) {
                future.get();
            }

            Integer reservedStock = jdbcTemplate.queryForObject("""
                SELECT reserved_stock
                FROM inventory
                WHERE campaign_id = ? AND product_id = ?
                """,
                    Integer.class,
                    campaignId,
                    productId
            );

            Integer reservationCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM reservations
                WHERE campaign_id = ? AND product_id = ?
                """,
                    Integer.class,
                    campaignId,
                    productId
            );

            assertThat(successfulReservations.get()).isEqualTo(10);
            assertThat(rejectedReservations.get()).isEqualTo(90);
            assertThat(reservedStock).isEqualTo(10);
            assertThat(reservationCount).isEqualTo(10);

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldReleaseReservedStockOnlyOnceWhenCancelAndExpireRace()
            throws Exception {

        Long userId = createTestUser();
        Long productId = createTestProduct();
        Long campaignId = createTestCampaign();

        createCampaignProduct(campaignId, productId);
        createInventory(campaignId, productId);

        String idempotencyKey = UUID.randomUUID().toString();

        CreateReservationRequest request =
                new CreateReservationRequest(campaignId, productId, 2);

        reservationService.createReservation(
                userId, request, idempotencyKey);

        Long reservationId = jdbcTemplate.queryForObject("""
            SELECT id
            FROM reservations
            WHERE idempotency_key = ?
            """, Long.class, idempotencyKey);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<?> cancelFuture = executor.submit(() -> {
                ready.countDown();
                start.await();
                reservationService.cancelReservation(reservationId);
                return null;
            });

            Future<?> expireFuture = executor.submit(() -> {
                ready.countDown();
                start.await();
                reservationService.expireReservation(reservationId);
                return null;
            });

            ready.await();
            start.countDown();

            cancelFuture.get();
            expireFuture.get();

            Integer reservedStock = jdbcTemplate.queryForObject("""
                SELECT reserved_stock
                FROM inventory
                WHERE campaign_id = ? AND product_id = ?
                """, Integer.class, campaignId, productId);

            String status = jdbcTemplate.queryForObject("""
                SELECT status
                FROM reservations
                WHERE id = ?
                """, String.class, reservationId);

            assertThat(reservedStock).isZero();

            assertThat(status)
                    .isIn("CANCELLED", "EXPIRED");

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldKeepReservationAndInventoryConsistentWhenConfirmAndExpireRace()
            throws Exception {

        Long userId = createTestUser();
        Long productId = createTestProduct();
        Long campaignId = createTestCampaign();

        createCampaignProduct(campaignId, productId);
        createInventory(campaignId, productId);

        String idempotencyKey = UUID.randomUUID().toString();

        CreateReservationRequest request =
                new CreateReservationRequest(campaignId, productId, 2);

        reservationService.createReservation(
                userId, request, idempotencyKey);

        Long reservationId = jdbcTemplate.queryForObject("""
            SELECT id
            FROM reservations
            WHERE idempotency_key = ?
            """, Long.class, idempotencyKey);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Boolean> confirmFuture = executor.submit(() -> {
                ready.countDown();
                start.await();

                try {
                    reservationService.confirmReservation(reservationId);
                    return true;
                } catch (IllegalStateException exception) {
                    return false;
                }
            });

            Future<?> expireFuture = executor.submit(() -> {
                ready.countDown();
                start.await();

                reservationService.expireReservation(reservationId);
                return null;
            });

            ready.await();
            start.countDown();

            boolean confirmed = confirmFuture.get();
            expireFuture.get();

            String status = jdbcTemplate.queryForObject("""
                SELECT status FROM reservations WHERE id = ?
                """, String.class, reservationId);

            Integer reservedStock = jdbcTemplate.queryForObject("""
                SELECT reserved_stock
                FROM inventory
                WHERE campaign_id = ? AND product_id = ?
                """, Integer.class, campaignId, productId);

            if (confirmed) {
                assertThat(status).isEqualTo("CONFIRMED");
                assertThat(reservedStock).isEqualTo(2);
            } else {
                assertThat(status).isEqualTo("EXPIRED");
                assertThat(reservedStock).isZero();
            }

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldReturnOriginalReservationForSameKeyAndSamePayload()
            throws Exception {

        Long userId = createTestUser();
        Long productId = createTestProduct();
        Long campaignId = createTestCampaign();

        createCampaignProduct(campaignId, productId);
        createInventory(campaignId, productId);

        String idempotencyKey = UUID.randomUUID().toString();

        CreateReservationRequest request =
                new CreateReservationRequest(campaignId, productId, 2);

        reservationService.createReservation(
                userId, request, idempotencyKey);

        Long originalReservationId = jdbcTemplate.queryForObject("""
            SELECT id
            FROM reservations
            WHERE idempotency_key = ?
            """, Long.class, idempotencyKey);

        reservationService.createReservation(
                userId, request, idempotencyKey);

        Long finalReservationId = jdbcTemplate.queryForObject("""
            SELECT id
            FROM reservations
            WHERE idempotency_key = ?
            """, Long.class, idempotencyKey);

        Integer reservationCount = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM reservations
            WHERE idempotency_key = ?
            """, Integer.class, idempotencyKey);

        Integer reservedStock = jdbcTemplate.queryForObject("""
            SELECT reserved_stock
            FROM inventory
            WHERE campaign_id = ? AND product_id = ?
            """, Integer.class, campaignId, productId);

        assertThat(finalReservationId).isEqualTo(originalReservationId);
        assertThat(reservationCount).isEqualTo(1);
        assertThat(reservedStock).isEqualTo(2);
    }

    @Test
    void shouldExpireOverdueReservationsAndReleaseStockOnlyOnce() {

        Long userId = createTestUser();
        Long productId = createTestProduct();
        Long campaignId = createTestCampaign();

        createCampaignProduct(campaignId, productId);
        createInventory(campaignId, productId);

        String idempotencyKey = "expiry-worker-" + UUID.randomUUID();

        CreateReservationRequest request =
                new CreateReservationRequest(campaignId, productId, 2);

        reservationService.createReservation(
                userId,
                request,
                idempotencyKey
        );

        Long reservationId = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM reservations
                WHERE idempotency_key = ?
                """,
                Long.class,
                idempotencyKey
        );

        // Simulate an expired reservation without waiting five minutes.
        jdbcTemplate.update(
                """
                UPDATE reservations
                SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second'
                WHERE id = ?
                """,
                reservationId
        );

        // Run the expiry worker.
        reservationExpiryWorker.expireOverdueReservations();

        // Run it again to verify stock is not released twice.
        reservationExpiryWorker.expireOverdueReservations();

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM reservations WHERE id = ?",
                String.class,
                reservationId
        );

        Integer reservedStock = jdbcTemplate.queryForObject(
                """
                SELECT reserved_stock
                FROM inventory
                WHERE campaign_id = ? AND product_id = ?
                """,
                Integer.class,
                campaignId,
                productId
        );

        assertThat(status).isEqualTo("EXPIRED");
        assertThat(reservedStock).isZero();
    }
}