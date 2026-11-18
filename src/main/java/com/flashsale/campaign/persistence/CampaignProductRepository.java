package com.flashsale.campaign.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public class CampaignProductRepository {

    private final JdbcTemplate jdbcTemplate;

    public CampaignProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<BigDecimal> findSalePrice(
            Long campaignId,
            Long productId) {

        return jdbcTemplate.query(
                """
                SELECT sale_price
                FROM campaign_products
                WHERE campaign_id = ?
                  AND product_id = ?
                """,
                rs -> rs.next()
                        ? Optional.of(rs.getBigDecimal("sale_price"))
                        : Optional.empty(),
                campaignId,
                productId
        );
    }
}