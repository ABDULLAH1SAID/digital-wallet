package com.example.digitalwallet.transaction.repository;

import com.example.digitalwallet.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t JOIN FETCH t.wallet WHERE t.idempotencyKey = :idempotencyKey")
    Optional<Transaction> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
}
