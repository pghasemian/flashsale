package com.flashsale.inventory.persistence;

import com.flashsale.inventory.domain.Inventory;
import com.flashsale.inventory.domain.InventoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository
        extends JpaRepository<Inventory, InventoryId> {
}