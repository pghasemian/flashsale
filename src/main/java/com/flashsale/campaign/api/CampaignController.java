package com.flashsale.campaign.api;

import com.flashsale.campaign.service.CampaignService;
import com.flashsale.dto.campaign.CampaignCreateRequest;
import com.flashsale.dto.campaign.CampaignResponse;
import com.flashsale.dto.campaign.CampaignUpdateRequest;
import com.flashsale.dto.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CampaignResponse> createCampaign(
            @Valid @RequestBody CampaignCreateRequest request
    ) {
        CampaignResponse campaign = campaignService.createCampaign(request);

        return ApiResponse.success(
                "Campaign created successfully",
                campaign
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<CampaignResponse> getCampaignById(
            @PathVariable Long id
    ) {
        CampaignResponse campaign = campaignService.getCampaignById(id);

        return ApiResponse.success(
                "Campaign retrieved successfully",
                campaign
        );
    }

    @GetMapping
    public ApiResponse<List<CampaignResponse>> getAllCampaigns() {
        List<CampaignResponse> campaigns = campaignService.getAllCampaigns();

        return ApiResponse.success(
                "Campaigns retrieved successfully",
                campaigns
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<CampaignResponse> updateCampaign(
            @PathVariable Long id,
            @Valid @RequestBody CampaignUpdateRequest request
    ) {
        CampaignResponse campaign = campaignService.updateCampaign(id, request);

        return ApiResponse.success(
                "Campaign updated successfully",
                campaign
        );
    }
}