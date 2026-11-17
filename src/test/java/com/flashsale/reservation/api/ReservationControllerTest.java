
package com.flashsale.reservation.api;

import com.flashsale.dto.reservation.ReservationResponse;
import com.flashsale.exception.ResourceNotFoundException;
import com.flashsale.reservation.application.ReservationService;
import com.flashsale.reservation.domain.ReservationStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    private ReservationResponse createReservation() {
        return new ReservationResponse(
                1L,
                200L,
                300L,
                2,
                ReservationStatus.ACTIVE,
                new BigDecimal("250.00"),
                Instant.now().plusSeconds(300),
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    void shouldGetMyReservationsSuccessfully() throws Exception {

        ReservationResponse reservation = createReservation();

        when(reservationService.getReservations(101L, false))
                .thenReturn(List.of(reservation));

        mockMvc.perform(get("/api/v1/reservations")
                        .with(jwt().jwt(jwt -> jwt.subject("101"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void shouldReturn404WhenReservationBelongsToAnotherUser() throws Exception {

        when(reservationService.getReservation(1L, 101L, false))
                .thenThrow(new ResourceNotFoundException("Reservation not found"));

        mockMvc.perform(get("/api/v1/reservations/1")
                        .with(jwt().jwt(jwt -> jwt.subject("101"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Reservation not found"));
    }

    @Test
    void shouldReturn401WhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(get("/api/v1/reservations"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservationService);
    }

    @Test
    void shouldGetMyReservationSuccessfully() throws Exception {

        ReservationResponse reservation = createReservation();

        when(reservationService.getReservation(1L, 101L, false))
                .thenReturn(reservation);

        mockMvc.perform(get("/api/v1/reservations/1")
                        .with(jwt().jwt(jwt -> jwt.subject("101"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void shouldAllowAdminToGetAllReservations() throws Exception {

        ReservationResponse reservation = createReservation();

        when(reservationService.getReservations(101L, true))
                .thenReturn(List.of(reservation));

        mockMvc.perform(get("/api/v1/reservations")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("101"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void shouldAllowAdminToGetAnyReservation() throws Exception {

        ReservationResponse reservation = createReservation();

        when(reservationService.getReservation(1L, 101L, true))
                .thenReturn(reservation);

        mockMvc.perform(get("/api/v1/reservations/1")
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("101"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }
}
