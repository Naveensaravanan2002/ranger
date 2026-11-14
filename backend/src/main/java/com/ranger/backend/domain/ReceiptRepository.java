package com.ranger.backend.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<LedgerReceipt, UUID> {
    List<LedgerReceipt> findByOwnerUidOrderByOccurredAtDesc(String ownerUid);
    Optional<LedgerReceipt> findByIdAndOwnerUid(UUID id, String ownerUid);
    boolean existsByOwnerUidAndSourceKey(String ownerUid, String sourceKey);
    long deleteByOwnerUid(String ownerUid);
}
