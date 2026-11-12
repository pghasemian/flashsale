package com.flashsale.campaign.service;

import com.flashsale.campaign.domain.Campaign;
import com.flashsale.campaign.domain.CampaignStatus;
import com.flashsale.campaign.persistence.CampaignRepository;
import com.flashsale.dto.campaign.CampaignCreateRequest;
import com.flashsale.dto.campaign.CampaignResponse;
import com.flashsale.dto.campaign.CampaignUpdateRequest;
import com.flashsale.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private CampaignService campaignService;

    @Test
    void shouldCreateCampaignSuccessfully() {

        Instant startTime = Instant.parse("2027-01-01T10:00:00Z");
        Instant endTime = Instant.parse("2027-01-07T22:00:00Z");

        CampaignCreateRequest request = new CampaignCreateRequest(
                "New Year Flash Sale",
                "Special New Year discounts",
                startTime,
                endTime
        );

        when(campaignRepository.save(any(Campaign.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CampaignResponse response = campaignService.createCampaign(request);

        assertEquals("New Year Flash Sale", response.name());
        assertEquals("Special New Year discounts", response.description());
        assertEquals(startTime, response.startTime());
        assertEquals(endTime, response.endTime());
        assertEquals("DRAFT", response.status());
    }

    @Test
    void shouldGetCampaignByIdSuccessfully() {

        Instant startTime = Instant.parse("2027-01-01T10:00:00Z");
        Instant endTime = Instant.parse("2027-01-07T22:00:00Z");

        Campaign campaign = Campaign.create(
                "New Year Flash Sale",
                "Special New Year discounts",
                startTime,
                endTime
        );

        when(campaignRepository.findById(1L))
                .thenReturn(Optional.of(campaign));

        CampaignResponse response =
                campaignService.getCampaignById(1L);

        assertEquals("New Year Flash Sale", response.name());
        assertEquals("Special New Year discounts", response.description());
        assertEquals(startTime, response.startTime());
        assertEquals(endTime, response.endTime());
        assertEquals("DRAFT", response.status());

        verify(campaignRepository).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenCampaignDoesNotExist() {

        when(campaignRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> campaignService.getCampaignById(999L));

        verify(campaignRepository).findById(999L);
    }

    @Test
    void shouldGetAllCampaignsSuccessfully() {
        Instant startTime = Instant.parse("2027-01-01T10:00:00Z");
        Instant endTime = Instant.parse("2027-01-07T22:00:00Z");

        Campaign campaign1 = Campaign.create(
                "New Year Sale",
                "New Year discounts",
                startTime,
                endTime
        );

        Campaign campaign2 = Campaign.create(
                "Winter Sale",
                "Winter discounts",
                startTime,
                endTime
        );

        when(campaignRepository.findAll())
                .thenReturn(List.of(campaign1, campaign2));

        List<CampaignResponse> response = campaignService.getAllCampaigns();

        assertEquals(2, response.size());
        assertEquals("New Year Sale", response.get(0).name());
        assertEquals("Winter Sale", response.get(1).name());

        verify(campaignRepository).findAll();
    }

    @Test
    void shouldReturnEmptyListWhenNoCampaignsExist() {
        when(campaignRepository.findAll()).thenReturn(List.of());

        List<CampaignResponse> response = campaignService.getAllCampaigns();

        assertTrue(response.isEmpty());

        verify(campaignRepository).findAll();
    }

    @Test
    void shouldUpdateDraftCampaignSuccessfully() {
        Instant startTime = Instant.parse("2027-02-01T10:00:00Z");
        Instant endTime = Instant.parse("2027-02-07T22:00:00Z");

        Campaign campaign = Campaign.create(
                "Old Campaign",
                "Old description",
                startTime,
                endTime
        );

        CampaignUpdateRequest request = new CampaignUpdateRequest(
                "Updated Campaign",
                "Updated description",
                startTime,
                endTime
        );

        when(campaignRepository.findById(1L))
                .thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any(Campaign.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CampaignResponse response = campaignService.updateCampaign(1L, request);

        assertEquals("Updated Campaign", response.name());
        assertEquals("Updated description", response.description());
        assertEquals(startTime, response.startTime());
        assertEquals(endTime, response.endTime());
        assertEquals("DRAFT", response.status());

        verify(campaignRepository).findById(1L);
        verify(campaignRepository).save(campaign);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistingCampaign() {
        Instant startTime = Instant.parse("2027-02-01T10:00:00Z");
        Instant endTime = Instant.parse("2027-02-07T22:00:00Z");

        CampaignUpdateRequest request = new CampaignUpdateRequest(
                "Updated Campaign",
                "Updated description",
                startTime,
                endTime
        );

        when(campaignRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> campaignService.updateCampaign(999L, request)
        );

        verify(campaignRepository).findById(999L);
        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonDraftCampaign() {
        Instant startTime = Instant.parse("2027-02-01T10:00:00Z");
        Instant endTime = Instant.parse("2027-02-07T22:00:00Z");

        Campaign campaign = Campaign.create(
                "Active Campaign",
                "Active description",
                startTime,
                endTime
        );
        campaign.setStatus(CampaignStatus.ACTIVE);

        CampaignUpdateRequest request = new CampaignUpdateRequest(
                "Updated Campaign",
                "Updated description",
                startTime,
                endTime
        );

        when(campaignRepository.findById(1L))
                .thenReturn(Optional.of(campaign));

        assertThrows(
                IllegalStateException.class,
                () -> campaignService.updateCampaign(1L, request)
        );

        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    void shouldThrowExceptionWhenEndTimeIsBeforeStartTime() {
        Instant startTime = Instant.parse("2027-02-07T22:00:00Z");
        Instant endTime = Instant.parse("2027-02-01T10:00:00Z");

        Campaign campaign = Campaign.create(
                "Old Campaign",
                "Old description",
                startTime,
                endTime
        );

        CampaignUpdateRequest request = new CampaignUpdateRequest(
                "Updated Campaign",
                "Updated description",
                startTime,
                endTime
        );

        when(campaignRepository.findById(1L))
                .thenReturn(Optional.of(campaign));

        assertThrows(
                IllegalArgumentException.class,
                () -> campaignService.updateCampaign(1L, request)
        );

        verify(campaignRepository, never()).save(any(Campaign.class));
    }

    @Test
    void shouldThrowExceptionWhenStartTimeEqualsEndTime() {
        Instant sameTime = Instant.parse("2027-02-01T10:00:00Z");

        Campaign campaign = Campaign.create(
                "Old Campaign",
                "Old description",
                sameTime,
                sameTime.plusSeconds(3600)
        );

        CampaignUpdateRequest request = new CampaignUpdateRequest(
                "Updated Campaign",
                "Updated description",
                sameTime,
                sameTime
        );

        when(campaignRepository.findById(1L))
                .thenReturn(Optional.of(campaign));

        assertThrows(
                IllegalArgumentException.class,
                () -> campaignService.updateCampaign(1L, request)
        );

        verify(campaignRepository, never()).save(any(Campaign.class));
    }
}