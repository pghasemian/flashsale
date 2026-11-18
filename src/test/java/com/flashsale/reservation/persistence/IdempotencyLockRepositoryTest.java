
package com.flashsale.reservation.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(IdempotencyLockRepository.class)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class IdempotencyLockRepositoryTest {

    @Autowired
    private IdempotencyLockRepository lockRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldSerializeTransactionsUsingSameIdempotencyKey()
            throws Exception {

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        String key = "concurrent-lock-test";

        CountDownLatch firstLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);

        AtomicBoolean secondAcquiredLock = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() ->
                    transactionTemplate.execute(status -> {
                        lockRepository.acquireTransactionLock(key);
                        firstLockAcquired.countDown();

                        try {
                            if (!releaseFirstTransaction.await(
                                    10, TimeUnit.SECONDS)) {
                                throw new IllegalStateException(
                                        "First transaction timed out"
                                );
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }

                        return null;
                    })
            );

            assertThat(firstLockAcquired.await(10, TimeUnit.SECONDS))
                    .isTrue();

            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();

                return transactionTemplate.execute(status -> {
                    lockRepository.acquireTransactionLock(key);
                    secondAcquiredLock.set(true);
                    return null;
                });
            });

            assertThat(secondStarted.await(10, TimeUnit.SECONDS))
                    .isTrue();

            // The first transaction still holds the lock.
            Thread.sleep(300);
            assertThat(secondAcquiredLock.get()).isFalse();

            releaseFirstTransaction.countDown();

            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);

            assertThat(secondAcquiredLock.get()).isTrue();

        } finally {
            releaseFirstTransaction.countDown();
            executor.shutdownNow();
        }
    }
}
