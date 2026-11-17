package com.flashsale.reservation.api;

import com.flashsale.dto.common.ApiResponse;
import com.flashsale.dto.reservation.ReservationResponse;
import com.flashsale.reservation.application.ReservationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public ApiResponse<List<ReservationResponse>> getReservations(
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        Long userId = Long.parseLong(jwt.getSubject());
        boolean isAdmin = isAdmin(authentication);

        List<ReservationResponse> reservations =
                reservationService.getReservations(userId, isAdmin);

        return ApiResponse.success(
                "Reservations retrieved successfully", reservations);
    }

    @GetMapping("/{id}")
    public ApiResponse<ReservationResponse> getReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        Long userId = Long.parseLong(jwt.getSubject());
        boolean isAdmin = isAdmin(authentication);

        ReservationResponse reservation =
                reservationService.getReservation(id, userId, isAdmin);

        return ApiResponse.success(
                "Reservation retrieved successfully", reservation);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));
    }
}