package org.example.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CallbackOutboxRepository extends JpaRepository<CallbackOutbox, Long> {

    @Query(value = """
            select * from callback_outbox
            where sent_at is null and attempts < :maxAttempts and send_at <= now()
            order by send_at
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<CallbackOutbox> findDue(int maxAttempts, int limit);
}
