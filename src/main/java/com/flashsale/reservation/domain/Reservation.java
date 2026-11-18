package com.flashsale.reservation.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Reservation() {
        // Required by JPA
    }

    public static Reservation create(
            Long userId,
            Long campaignId,
            Long productId,
            int quantity,
            BigDecimal unitPrice,
            String idempotencyKey
    ) {
        Reservation reservation = new Reservation();

        reservation.userId = userId;
        reservation.campaignId = campaignId;
        reservation.productId = productId;
        reservation.quantity = quantity;
        reservation.unitPrice = unitPrice;
        reservation.status = ReservationStatus.ACTIVE;
        reservation.expiresAt = Instant.now().plusSeconds(300);
        reservation.idempotencyKey = idempotencyKey;

        return reservation;
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

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public boolean matchesRequest(
            Long userId,
            Long campaignId,
            Long productId,
            int quantity) {

        return this.userId.equals(userId)
                && this.campaignId.equals(campaignId)
                && this.productId.equals(productId)
                && this.quantity == quantity;
    }

    public void confirm() {
        if (status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only active reservations can be confirmed");
        }

        status = ReservationStatus.CONFIRMED;
    }

    public void expire() {
        if (status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only active reservations can expire");
        }

        status = ReservationStatus.EXPIRED;
    }

    public void cancel() {
        if (status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only active reservations can be cancelled");
        }

        status = ReservationStatus.CANCELLED;
    }
}