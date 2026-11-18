package com.flashsale.inventory.persistence;

import com.flashsale.inventory.domain.Inventory;
import com.flashsale.inventory.domain.InventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository
        extends JpaRepository<Inventory, InventoryId> {

    @Modifying
    @Query(value = """
            UPDATE inventory
            SET reserved_stock = reserved_stock + :quantity,
                updated_at = CURRENT_TIMESTAMP
            WHERE campaign_id = :campaignId
              AND product_id = :productId
              AND total_stock - reserved_stock >= :quantity
            """, nativeQuery = true)
    int reserveStock(
            @Param("campaignId") Long campaignId,
            @Param("productId") Long productId,
            @Param("quantity") int quantity
    );

    @Modifying
    @Query(value = """
        UPDATE inventory
        SET reserved_stock = reserved_stock - :quantity,
            updated_at = CURRENT_TIMESTAMP
        WHERE campaign_id = :campaignId
          AND product_id = :productId
          AND reserved_stock >= :quantity
        """, nativeQuery = true)
    int releaseStock(
            @Param("campaignId") Long campaignId,
            @Param("productId") Long productId,
            @Param("quantity") int quantity
    );
}