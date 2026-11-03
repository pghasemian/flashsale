package com.flashsale.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

@Entity
@Table(name = "inventory")
public class Inventory {

    @EmbeddedId
    private InventoryId id;

    @Column(name = "total_stock", nullable = false)
    private int totalStock;

    @Column(name = "reserved_stock", nullable = false)
    private int reservedStock = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Inventory() {
        // Required by JPA
    }

    public static Inventory create(
            Long campaignId,
            Long productId,
            int totalStock
    ) {
        Inventory inventory = new Inventory();
        inventory.id = new InventoryId(campaignId, productId);
        inventory.totalStock = totalStock;
        inventory.reservedStock = 0;
        return inventory;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public InventoryId getId() {
        return id;
    }

    public int getTotalStock() {
        return totalStock;
    }

    public int getReservedStock() {
        return reservedStock;
    }

    public int getAvailableStock() {
        return totalStock - reservedStock;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}