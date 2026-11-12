package com.flashsale.campaign.service;

import com.flashsale.campaign.domain.Campaign;
import com.flashsale.campaign.domain.CampaignStatus;
import com.flashsale.campaign.persistence.CampaignRepository;
import com.flashsale.dto.campaign.CampaignCreateRequest;
import com.flashsale.dto.campaign.CampaignResponse;
import com.flashsale.dto.campaign.CampaignUpdateRequest;
import com.flashsale.exception.ResourceNotFoundException;
import com.flashsale.mapper.CampaignMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;

    public CampaignService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Transactional
    public CampaignResponse createCampaign(CampaignCreateRequest request) {

        Campaign campaign = Campaign.create(
                request.name(),
                request.description(),
                request.startTime(),
                request.endTime()
        );

        Campaign savedCampaign = campaignRepository.save(campaign);

        return CampaignMapper.toResponse(savedCampaign);
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(Long id) {

        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campaign not found with id: " + id
                ));

        return CampaignMapper.toResponse(campaign);
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> getAllCampaigns() {

        return campaignRepository.findAll()
                .stream()
                .map(CampaignMapper::toResponse)
                .toList();
    }

    @Transactional
    public CampaignResponse updateCampaign(
            Long id,
            CampaignUpdateRequest request
    ) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campaign not found with id: " + id
                ));

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT campaigns can be updated"
            );
        }

        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException(
                    "Campaign end time must be after start time"
            );
        }

        campaign.setName(request.name());
        campaign.setDescription(request.description());
        campaign.setStartTime(request.startTime());
        campaign.setEndTime(request.endTime());

        Campaign updatedCampaign = campaignRepository.save(campaign);

        return CampaignMapper.toResponse(updatedCampaign);
    }
}