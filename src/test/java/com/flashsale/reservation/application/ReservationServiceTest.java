
package com.flashsale.reservation.application;

import com.flashsale.campaign.persistence.CampaignProductRepository;
import com.flashsale.dto.reservation.CreateReservationRequest;
import com.flashsale.dto.reservation.ReservationResponse;
import com.flashsale.exception.ResourceNotFoundException;
import com.flashsale.inventory.persistence.InventoryRepository;
import com.flashsale.reservation.domain.Reservation;
import com.flashsale.reservation.persistence.IdempotencyLockRepository;
import com.flashsale.reservation.persistence.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private CampaignProductRepository campaignProductRepository;

    @Mock
    private IdempotencyLockRepository idempotencyLockRepository;

    @Test
    void shouldReturnOnlyMyReservationsForUser() {

        Reservation reservation = Reservation.create(
                101L,
                200L,
                300L,
                2,
                new BigDecimal("250.00"),
                "test-idempotency-key"
        );
        when(reservationRepository.findByUserId(101L))
                .thenReturn(List.of(reservation));

        var result = reservationService.getReservations(101L, false);

        assertEquals(1, result.size());
        verify(reservationRepository).findByUserId(101L);
        verify(reservationRepository, never()).findAll();
    }

    @Test
    void shouldReturnAllReservationsForAdmin() {

        Reservation reservation = Reservation.create(
                101L,
                200L,
                300L,
                2,
                new BigDecimal("250.00"),
                "test-idempotency-key"
        );
        when(reservationRepository.findAll())
                .thenReturn(List.of(reservation));

        var result = reservationService.getReservations(101L, true);

        assertEquals(1, result.size());
        verify(reservationRepository).findAll();
        verify(reservationRepository, never()).findByUserId(anyLong());
    }

    @Test
    void shouldReturnMyReservation() {

        Reservation reservation = Reservation.create(
                101L,
                200L,
                300L,
                2,
                new BigDecimal("250.00"),
                "test-idempotency-key"
        );

        when(reservationRepository.findByIdAndUserId(1L, 101L))
                .thenReturn(Optional.of(reservation));

        var result = reservationService.getReservation(1L, 101L, false);

        assertNotNull(result);
        verify(reservationRepository).findByIdAndUserId(1L, 101L);
        verify(reservationRepository, never()).findById(anyLong());
    }

    @Test
    void shouldThrowNotFoundWhenReservationBelongsToAnotherUser() {

        when(reservationRepository.findByIdAndUserId(1L, 101L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> reservationService.getReservation(1L, 101L, false)
        );

        verify(reservationRepository).findByIdAndUserId(1L, 101L);
    }

    @Test
    void shouldAllowAdminToGetAnyReservation() {

        Reservation reservation = Reservation.create(
                101L,
                200L,
                300L,
                2,
                new BigDecimal("250.00"),
                "test-idempotency-key"
        );
        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        var result = reservationService.getReservation(1L, 101L, true);

        assertNotNull(result);
        verify(reservationRepository).findById(1L);
        verify(reservationRepository, never())
                .findByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void shouldThrowNotFoundWhenReservationDoesNotExistForAdmin() {

        when(reservationRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> reservationService.getReservation(1L, 101L, true)
        );

        verify(reservationRepository).findById(1L);
    }


    @Test
    void shouldReturnExistingReservationWhenSameIdempotencyKeyAndPayloadAreUsed() {
        Long userId = 1L;
        Long campaignId = 10L;
        Long productId = 20L;
        int quantity = 2;
        String idempotencyKey = "same-request-key";

        Reservation existingReservation = Reservation.create(
                userId,
                campaignId,
                productId,
                quantity,
                new BigDecimal("19.99"),
                idempotencyKey
        );

        when(reservationRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingReservation));

        CreateReservationRequest request = new CreateReservationRequest(
                campaignId,
                productId,
                quantity
        );

        ReservationResponse response = reservationService.createReservation(
                userId,
                request,
                idempotencyKey
        );

        assertThat(response).isNotNull();
        assertThat(response.campaignId()).isEqualTo(campaignId);
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.quantity()).isEqualTo(quantity);

        verify(reservationRepository, never()).saveAndFlush(any());
        verify(inventoryRepository, never()).reserveStock(any(), any(), anyInt());
        verify(campaignProductRepository, never()).findSalePrice(any(), any());
        verify(idempotencyLockRepository).acquireTransactionLock(idempotencyKey);
    }


    @Test
    void shouldRejectSameIdempotencyKeyWithDifferentPayload() {
        Long userId = 1L;
        Long campaignId = 10L;
        Long productId = 20L;
        String idempotencyKey = "same-request-key";

        Reservation existingReservation = Reservation.create(
                userId,
                campaignId,
                productId,
                2,
                new BigDecimal("19.99"),
                idempotencyKey
        );

        when(reservationRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingReservation));

        CreateReservationRequest differentRequest = new CreateReservationRequest(
                campaignId,
                productId,
                3
        );

        assertThatThrownBy(() ->
                reservationService.createReservation(
                        userId,
                        differentRequest,
                        idempotencyKey
                )
        ).isInstanceOf(IllegalArgumentException.class);

        verify(reservationRepository, never()).saveAndFlush(any());
        verify(inventoryRepository, never()).reserveStock(any(), any(), anyInt());
    }


    @Test
    void shouldRejectSameIdempotencyKeyUsedByDifferentUser() {
        Long originalUserId = 1L;
        Long anotherUserId = 2L;
        Long campaignId = 10L;
        Long productId = 20L;
        String idempotencyKey = "shared-request-key";

        Reservation existingReservation = Reservation.create(
                originalUserId,
                campaignId,
                productId,
                2,
                new BigDecimal("19.99"),
                idempotencyKey
        );

        when(reservationRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingReservation));

        CreateReservationRequest request = new CreateReservationRequest(
                campaignId,
                productId,
                2
        );

        assertThatThrownBy(() ->
                reservationService.createReservation(
                        anotherUserId,
                        request,
                        idempotencyKey
                )
        ).isInstanceOf(IllegalArgumentException.class);

        verify(reservationRepository, never()).saveAndFlush(any());
        verify(inventoryRepository, never()).reserveStock(any(), any(), anyInt());
    }

}
