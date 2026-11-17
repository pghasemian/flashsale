
package com.flashsale.reservation.application;

import com.flashsale.exception.ResourceNotFoundException;
import com.flashsale.reservation.domain.Reservation;
import com.flashsale.reservation.persistence.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void shouldReturnOnlyMyReservationsForUser() {

        Reservation reservation = Reservation.create(
                101L, 200L, 300L, 2, new BigDecimal("250.00"));

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
                101L, 200L, 300L, 2, new BigDecimal("250.00"));

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
                101L, 200L, 300L, 2, new BigDecimal("250.00"));

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
                202L, 200L, 300L, 2, new BigDecimal("250.00"));

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
}
