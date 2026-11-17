
package com.flashsale.reservation.application;

import com.flashsale.dto.reservation.ReservationResponse;
import com.flashsale.exception.ResourceNotFoundException;
import com.flashsale.mapper.ReservationMapper;
import com.flashsale.reservation.domain.Reservation;
import com.flashsale.reservation.persistence.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public ReservationService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
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
}
