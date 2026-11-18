
package com.flashsale.reservation.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyLockRepository {

    private final JdbcTemplate jdbcTemplate;

    public IdempotencyLockRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void acquireTransactionLock(String idempotencyKey) {
        jdbcTemplate.query(
                "SELECT pg_advisory_xact_lock(hashtext(?))",
                resultSet -> null,
                idempotencyKey
        );
    }
}
