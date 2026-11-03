package com.flashsale.campaign.persistence;

import com.flashsale.campaign.domain.Campaign;
import com.flashsale.campaign.domain.CampaignStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CampaignRepositoryTest {

    @Autowired
    private CampaignRepository campaignRepository;

    @Test
    void shouldSaveAndFindCampaignByStatus() {
        Instant startTime = Instant.now().plusSeconds(3600);
        Instant endTime = startTime.plusSeconds(86400);

        Campaign campaign = Campaign.create(
                "Summer Flash Sale",
                "Summer promotional campaign",
                startTime,
                endTime
        );

        Campaign savedCampaign = campaignRepository.save(campaign);

        List<Campaign> campaigns =
                campaignRepository.findByStatus(CampaignStatus.DRAFT);

        assertThat(savedCampaign.getId()).isNotNull();
        assertThat(campaigns)
                .extracting(Campaign::getName)
                .contains("Summer Flash Sale");

        Campaign foundCampaign = campaigns.stream()
                .filter(item -> item.getId().equals(savedCampaign.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(foundCampaign.getStartTime()).isEqualTo(startTime);
        assertThat(foundCampaign.getEndTime()).isEqualTo(endTime);
        assertThat(foundCampaign.getStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(foundCampaign.getCreatedAt()).isNotNull();
        assertThat(foundCampaign.getUpdatedAt()).isNotNull();
    }
}