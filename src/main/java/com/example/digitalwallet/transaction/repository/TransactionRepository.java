package com.example.digitalwallet.transaction.repository;

import com.example.digitalwallet.transaction.entity.Transaction;
import com.example.digitalwallet.transaction.entity.TransactionOperation;
import com.example.digitalwallet.transaction.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("""
            SELECT t FROM Transaction t
            JOIN FETCH t.wallet
            WHERE t.wallet.id = :walletId
              AND t.idempotencyKey = :idempotencyKey
              AND t.operation = :operation
            """)
    Optional<Transaction> findByWalletIdAndIdempotencyKeyAndOperation(
            @Param("walletId") Long walletId,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("operation") TransactionOperation operation
    );

    @Query("SELECT t FROM Transaction t JOIN FETCH t.wallet WHERE t.referenceId = :referenceId")
    List<Transaction> findByReferenceId(@Param("referenceId") String referenceId);

    @Query(
            value = "SELECT t FROM Transaction t JOIN FETCH t.wallet WHERE t.wallet.id = :walletId AND (:type IS NULL OR t.type = :type)",
            countQuery = "SELECT COUNT(t) FROM Transaction t WHERE t.wallet.id = :walletId AND (:type IS NULL OR t.type = :type)"
    )
    Page<Transaction> findByWalletIdAndType(
            @Param("walletId") Long walletId,
            @Param("type") TransactionType type,
            Pageable pageable
    );
}