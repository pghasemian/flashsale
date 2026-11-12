package com.flashsale.campaign.api;

import com.flashsale.campaign.service.CampaignService;
import com.flashsale.dto.campaign.CampaignCreateRequest;
import com.flashsale.dto.campaign.CampaignResponse;
import com.flashsale.dto.campaign.CampaignUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CampaignControllerTest {

    @Mock
    private CampaignService campaignService;

    @InjectMocks
    private CampaignController campaignController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(campaignController)
                .build();
    }

    @Test
    void createCampaignSuccessfully() throws Exception {

        Instant startTime = Instant.parse("2027-01-01T10:00:00Z");
        Instant endTime = Instant.parse("2027-01-02T10:00:00Z");

        CampaignCreateRequest request = new CampaignCreateRequest(
                "Black Friday",
                "Black Friday Campaign",
                startTime,
                endTime
        );

        CampaignResponse response = new CampaignResponse(
                1L,
                "Black Friday",
                "Black Friday Campaign",
                startTime,
                endTime,
                "DRAFT",
                Instant.now(),
                Instant.now()
        );

        when(campaignService.createCampaign(request)).thenReturn(response);

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Black Friday",
                                  "description": "Black Friday Campaign",
                                  "startTime": "2027-01-01T10:00:00Z",
                                  "endTime": "2027-01-02T10:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Black Friday"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void getCampaignByIdSuccessfully() throws Exception {

        Instant startTime = Instant.parse("2027-01-01T10:00:00Z");
        Instant endTime = Instant.parse("2027-01-02T10:00:00Z");

        CampaignResponse response = new CampaignResponse(
                1L,
                "Black Friday",
                "Black Friday Campaign",
                startTime,
                endTime,
                "DRAFT",
                Instant.now(),
                Instant.now()
        );

        when(campaignService.getCampaignById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/campaigns/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Black Friday"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void getAllCampaignsSuccessfully() throws Exception {

        Instant startTime = Instant.parse("2027-01-01T10:00:00Z");
        Instant endTime = Instant.parse("2027-01-02T10:00:00Z");

        CampaignResponse campaign1 = new CampaignResponse(
                1L,
                "Black Friday",
                "Black Friday Campaign",
                startTime,
                endTime,
                "DRAFT",
                Instant.now(),
                Instant.now()
        );

        CampaignResponse campaign2 = new CampaignResponse(
                2L,
                "Summer Sale",
                "Summer Sale Campaign",
                startTime,
                endTime,
                "SCHEDULED",
                Instant.now(),
                Instant.now()
        );

        when(campaignService.getAllCampaigns())
                .thenReturn(List.of(campaign1, campaign2));

        mockMvc.perform(get("/api/v1/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Black Friday"))
                .andExpect(jsonPath("$.data[1].name").value("Summer Sale"));
    }

    @Test
    void updateCampaignSuccessfully() throws Exception {

        Instant startTime = Instant.parse("2027-02-01T10:00:00Z");
        Instant endTime = Instant.parse("2027-02-02T10:00:00Z");

        CampaignUpdateRequest request = new CampaignUpdateRequest(
                "Updated Black Friday",
                "Updated description",
                startTime,
                endTime
        );

        CampaignResponse response = new CampaignResponse(
                1L,
                "Updated Black Friday",
                "Updated description",
                startTime,
                endTime,
                "DRAFT",
                Instant.now(),
                Instant.now()
        );

        when(campaignService.updateCampaign(1L, request))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/campaigns/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Updated Black Friday",
                              "description": "Updated description",
                              "startTime": "2027-02-01T10:00:00Z",
                              "endTime": "2027-02-02T10:00:00Z"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Updated Black Friday"))
                .andExpect(jsonPath("$.data.description").value("Updated description"));
    }
}