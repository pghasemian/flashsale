package com.flashsale.inventory.persistence;

import com.flashsale.inventory.domain.Inventory;
import com.flashsale.inventory.domain.InventoryId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void shouldSaveAndFindInventoryByCompositeId() {
        Long campaignId = 1L;
        Long productId = 1L;

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
}