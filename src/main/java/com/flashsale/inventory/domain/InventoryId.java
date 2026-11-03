package com.flashsale.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class InventoryId implements Serializable {

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "product_id")
    private Long productId;

    protected InventoryId() {
        // Required by JPA
    }

    public InventoryId(Long campaignId, Long productId) {
        this.campaignId = campaignId;
        this.productId = productId;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public Long getProductId() {
        return productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryId that)) return false;
        return Objects.equals(campaignId, that.campaignId)
                && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(campaignId, productId);
    }
}