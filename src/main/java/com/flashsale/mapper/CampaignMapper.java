package com.flashsale.mapper;

import com.flashsale.campaign.domain.Campaign;
import com.flashsale.dto.campaign.CampaignResponse;

public class CampaignMapper {

    private CampaignMapper() {
    }

    public static CampaignResponse toResponse(Campaign campaign) {

        return new CampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getStartTime(),
                campaign.getEndTime(),
                campaign.getStatus().name(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}