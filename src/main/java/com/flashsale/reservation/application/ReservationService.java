
package com.flashsale.reservation.application;

import com.flashsale.campaign.persistence.CampaignProductRepository;
import com.flashsale.dto.reservation.CreateReservationRequest;
import com.flashsale.dto.reservation.ReservationResponse;
import com.flashsale.exception.ResourceNotFoundException;
import com.flashsale.inventory.persistence.InventoryRepository;
import com.flashsale.mapper.ReservationMapper;
import com.flashsale.reservation.domain.Reservation;
import com.flashsale.reservation.domain.ReservationStatus;
import com.flashsale.reservation.persistence.IdempotencyLockRepository;
import com.flashsale.reservation.persistence.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;
    private final CampaignProductRepository campaignProductRepository;
    private final IdempotencyLockRepository idempotencyLockRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            InventoryRepository inventoryRepository,
            CampaignProductRepository campaignProductRepository,
            IdempotencyLockRepository idempotencyLockRepository) {

        this.reservationRepository = reservationRepository;
        this.inventoryRepository = inventoryRepository;
        this.campaignProductRepository = campaignProductRepository;
        this.idempotencyLockRepository = idempotencyLockRepository;
    }

    public List<ReservationResponse> getReservations(
            Long userId, boolean isAdmin) {

        List<Reservation> reservations = isAdmin
                ? reservationRepository.findAll()
                : reservationRepository.findByUserId(userId);

        return reservations.stream()
                .map(ReservationMapper::toResponse)
                .toList();
    }

    public ReservationResponse getReservation(
            Long reservationId, Long userId, boolean isAdmin) {

        Reservation reservation = isAdmin
                ? reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Reservation not found"))
                : reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Reservation not found"));

        return ReservationMapper.toResponse(reservation);
    }

    @Transactional
    public ReservationResponse createReservation(
            Long userId,
            CreateReservationRequest request,
            String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException("Invalid Idempotency-Key");
        }

        idempotencyLockRepository.acquireTransactionLock(idempotencyKey);

        var existingReservation = reservationRepository.findByIdempotencyKey(idempotencyKey);

        if (existingReservation.isPresent()) {
            Reservation existing = existingReservation.get();

            if (!existing.matchesRequest(
                    userId,
                    request.campaignId(),
                    request.productId(),
                    request.quantity())) {
                throw new IllegalArgumentException(
                        "Idempotency key was already used with a different request");
            }

            return ReservationMapper.toResponse(existing);
        }

        BigDecimal salePrice = campaignProductRepository
                .findSalePrice(request.campaignId(), request.productId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Campaign product not found"));

        int updatedRows = inventoryRepository.reserveStock(
                request.campaignId(),
                request.productId(),
                request.quantity());

        if (updatedRows == 0) {
            throw new IllegalStateException("Insufficient stock");
        }

        Reservation reservation = Reservation.create(
                userId,
                request.campaignId(),
                request.productId(),
                request.quantity(),
                salePrice,
                idempotencyKey);

        Reservation savedReservation =
                reservationRepository.saveAndFlush(reservation);

        return ReservationMapper.toResponse(savedReservation);
    }

    @Transactional
    public void cancelReservation(Long reservationId) {

        releaseReservation(
                reservationId,
                ReservationStatus.CANCELLED
        );
    }

    @Transactional
    public void expireReservation(Long reservationId) {

        releaseReservation(
                reservationId,
                ReservationStatus.EXPIRED
        );
    }

    private void releaseReservation(
            Long reservationId,
            ReservationStatus targetStatus) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Reservation not found"));

        int updatedRows = reservationRepository.updateStatusIfCurrent(
                reservationId,
                ReservationStatus.ACTIVE,
                targetStatus
        );

        if (updatedRows == 0) {
            return;
        }

        int releasedRows = inventoryRepository.releaseStock(
                reservation.getCampaignId(),
                reservation.getProductId(),
                reservation.getQuantity()
        );

        if (releasedRows == 0) {
            throw new IllegalStateException(
                    "Could not release reserved stock");
        }
    }

    @Transactional
    public void confirmReservation(Long reservationId) {

        int updatedRows = reservationRepository.updateStatusIfCurrent(
                reservationId,
                ReservationStatus.ACTIVE,
                ReservationStatus.CONFIRMED
        );

        if (updatedRows == 0) {
            throw new IllegalStateException(
                    "Reservation is no longer active"
            );
        }
    }
}
