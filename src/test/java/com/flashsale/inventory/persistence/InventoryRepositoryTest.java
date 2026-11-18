package com.flashsale.inventory.persistence;

import com.flashsale.campaign.domain.Campaign;
import com.flashsale.campaign.persistence.CampaignRepository;
import com.flashsale.catalog.domain.Product;
import com.flashsale.catalog.persistence.ProductRepository;
import com.flashsale.inventory.domain.Inventory;
import com.flashsale.inventory.domain.InventoryId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldSaveAndFindInventoryByCompositeId() {
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

        Long campaignId = campaign.getId();
        Long productId = product.getId();

        Inventory inventory = Inventory.create(
                campaignId,
                productId,
                100
        );

        Inventory savedInventory = inventoryRepository.save(inventory);

        InventoryId inventoryId = new InventoryId(campaignId, productId);

        Optional<Inventory> foundInventory =
                inventoryRepository.findById(inventoryId);

        assertThat(savedInventory.getId()).isNotNull();
        assertThat(foundInventory).isPresent();

        Inventory result = foundInventory.orElseThrow();

        assertThat(result.getTotalStock()).isEqualTo(100);
        assertThat(result.getReservedStock()).isZero();
        assertThat(result.getAvailableStock()).isEqualTo(100);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldReserveStockWhenEnoughStockIsAvailable() {

        Inventory inventory = createTestInventory(10);

        int result = inventoryRepository.reserveStock(
                inventory.getId().getCampaignId(),
                inventory.getId().getProductId(),
                4
        );

        Integer reservedStock = jdbcTemplate.queryForObject(
                """
                SELECT reserved_stock
                FROM inventory
                WHERE campaign_id = ? AND product_id = ?
                """,
                Integer.class,
                inventory.getId().getCampaignId(),
                inventory.getId().getProductId()
        );

        assertThat(result).isEqualTo(1);
        assertThat(reservedStock).isEqualTo(4);
    }

    @Test
    void shouldNotReserveStockWhenInsufficientStock() {

        Inventory inventory = createTestInventory(5);

        int result = inventoryRepository.reserveStock(
                inventory.getId().getCampaignId(),
                inventory.getId().getProductId(),
                6
        );

        Integer reservedStock = jdbcTemplate.queryForObject(
                """
                SELECT reserved_stock
                FROM inventory
                WHERE campaign_id = ? AND product_id = ?
                """,
                Integer.class,
                inventory.getId().getCampaignId(),
                inventory.getId().getProductId()
        );

        assertThat(result).isZero();
        assertThat(reservedStock).isZero();
    }

    private Inventory createTestInventory(int totalStock) {

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
                totalStock
        );

        return inventoryRepository.save(inventory);
    }

    @Test
    void shouldReleaseReservedStock() {

        Inventory inventory = createTestInventory(10);

        Long campaignId = inventory.getId().getCampaignId();
        Long productId = inventory.getId().getProductId();

        inventoryRepository.reserveStock(campaignId, productId, 6);

        int result = inventoryRepository.releaseStock(
                campaignId,
                productId,
                4
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

        assertThat(result).isEqualTo(1);
        assertThat(reservedStock).isEqualTo(2);
    }

    @Test
    void shouldNotReleaseMoreThanReservedStock() {

        Inventory inventory = createTestInventory(10);

        Long campaignId = inventory.getId().getCampaignId();
        Long productId = inventory.getId().getProductId();

        inventoryRepository.reserveStock(campaignId, productId, 3);

        int result = inventoryRepository.releaseStock(
                campaignId,
                productId,
                5
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

        assertThat(result).isZero();
        assertThat(reservedStock).isEqualTo(3);
    }


    @Test
    void shouldReserveOnlyAvailableStockUnderConcurrency() throws Exception {
        Inventory inventory = createTestInventory(10);

        Long campaignId = inventory.getId().getCampaignId();
        Long productId = inventory.getId().getProductId();

        // Commit setup data so concurrent transactions can see it.
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        int workerCount = 20;
        int requestCount = 100;

        ExecutorService executor =
                Executors.newFixedThreadPool(workerCount);

        CountDownLatch ready = new CountDownLatch(workerCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Integer>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();

                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Could not start concurrent requests"
                        );
                    }

                    Integer result = transactionTemplate.execute(status ->
                            inventoryRepository.reserveStock(
                                    campaignId,
                                    productId,
                                    1
                            )
                    );

                    return result == null ? 0 : result;
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

            // Release all worker threads at approximately the same time.
            start.countDown();

            int successfulReservations = 0;

            for (Future<Integer> future : futures) {
                successfulReservations += future.get(30, TimeUnit.SECONDS);
            }

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

            assertThat(successfulReservations).isEqualTo(10);
            assertThat(reservedStock).isEqualTo(10);
            assertThat(reservedStock).isLessThanOrEqualTo(10);

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

}