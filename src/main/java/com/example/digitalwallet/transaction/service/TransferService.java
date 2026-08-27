package com.example.digitalwallet.transaction.service;

import com.example.digitalwallet.common.exception.InsufficientBalanceException;
import com.example.digitalwallet.common.exception.InvalidRequestException;
import com.example.digitalwallet.common.exception.WalletNotFoundException;
import com.example.digitalwallet.transaction.dto.TransactionResponse;
import com.example.digitalwallet.transaction.dto.TransferRequest;
import com.example.digitalwallet.transaction.dto.TransferResponse;
import com.example.digitalwallet.transaction.dto.TransferResult;
import com.example.digitalwallet.transaction.entity.Transaction;
import com.example.digitalwallet.transaction.entity.TransactionOperation;
import com.example.digitalwallet.transaction.entity.TransactionStatus;
import com.example.digitalwallet.transaction.entity.TransactionType;
import com.example.digitalwallet.transaction.repository.TransactionRepository;
import com.example.digitalwallet.wallet.entity.Wallet;
import com.example.digitalwallet.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final PlatformTransactionManager transactionManager;

    public TransferResult transfer(Long userId, String idempotencyKey, TransferRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidRequestException("Idempotency-Key header is missing");
        }

        String key = idempotencyKey.trim();

        Wallet sender = walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);

        Optional<Transaction> existing = transactionRepository
                .findByWalletIdAndIdempotencyKeyAndOperation(
                        sender.getId(),
                        key,
                        TransactionOperation.TRANSFER
                );
        if (existing.isPresent()) {
            return new TransferResult(toTransferResponse(existing.get()), false);
        }

        validateRequest(request);

        Wallet receiver = walletRepository.findById(request.receiverWalletId())
                .orElseThrow(() -> new WalletNotFoundException(request.receiverWalletId()));

        if (sender.getId().equals(receiver.getId())) {
            throw new InvalidRequestException("Cannot transfer to the same wallet");
        }

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        try {
            TransferResponse created = txTemplate.execute(status ->
                    executeTransfer(sender.getId(), receiver.getId(), key, request.amount())
            );
            return new TransferResult(created, true);
        } catch (DataIntegrityViolationException ex) {
            return transactionRepository
                    .findByWalletIdAndIdempotencyKeyAndOperation(
                            sender.getId(),
                            key,
                            TransactionOperation.TRANSFER
                    )
                    .map(transaction -> new TransferResult(toTransferResponse(transaction), false))
                    .orElseThrow(() -> ex);
        }
    }

    private TransferResponse executeTransfer(Long senderId, Long receiverId, String idempotencyKey, BigDecimal amount) {
        Wallet sender;
        Wallet receiver;
        if (senderId < receiverId) {
            sender = lockWallet(senderId);
            receiver = lockWallet(receiverId);
        } else {
            receiver = lockWallet(receiverId);
            sender = lockWallet(senderId);
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }

        sender.debit(amount);
        receiver.credit(amount);
        walletRepository.save(sender);
        walletRepository.save(receiver);

        String referenceId = nextTransferReferenceId();

        Transaction debit = new Transaction();
        debit.setWallet(sender);
        debit.setType(TransactionType.DEBIT);
        debit.setAmount(amount);
        debit.setBalanceAfter(sender.getBalance());
        debit.setCounterpartyWallet(receiver);
        debit.setReferenceId(referenceId);
        debit.setStatus(TransactionStatus.COMPLETED);
        debit.setOperation(TransactionOperation.TRANSFER);
        debit.setIdempotencyKey(idempotencyKey);

        Transaction credit = new Transaction();
        credit.setWallet(receiver);
        credit.setType(TransactionType.CREDIT);
        credit.setAmount(amount);
        credit.setBalanceAfter(receiver.getBalance());
        credit.setCounterpartyWallet(sender);
        credit.setReferenceId(referenceId);
        credit.setStatus(TransactionStatus.COMPLETED);
        credit.setOperation(TransactionOperation.TRANSFER);

        Transaction savedDebit = transactionRepository.saveAndFlush(debit);
        Transaction savedCredit = transactionRepository.saveAndFlush(credit);

        return new TransferResponse(
                referenceId,
                amount,
                TransactionStatus.COMPLETED,
                TransactionResponse.from(savedDebit),
                TransactionResponse.from(savedCredit)
        );
    }

    private Wallet lockWallet(Long walletId) {
        return walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }

    private TransferResponse toTransferResponse(Transaction existing) {
        List<Transaction> legs = transactionRepository.findByReferenceId(existing.getReferenceId());
        Transaction debit = legs.stream()
                .filter(transaction -> transaction.getType() == TransactionType.DEBIT)
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Idempotency-Key already used for another operation"));
        Transaction credit = legs.stream()
                .filter(transaction -> transaction.getType() == TransactionType.CREDIT)
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Idempotency-Key already used for another operation"));

        return new TransferResponse(
                existing.getReferenceId(),
                debit.getAmount(),
                TransactionStatus.COMPLETED,
                TransactionResponse.from(debit),
                TransactionResponse.from(credit)
        );
    }

    private void validateRequest(TransferRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Request body is required");
        }
        if (request.receiverWalletId() == null || request.receiverWalletId() <= 0) {
            throw new InvalidRequestException("Receiver wallet id is required");
        }
        if (request.amount() == null) {
            throw new InvalidRequestException("Amount is required");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Amount must be greater than zero");
        }
        if (request.amount().scale() > 2) {
            throw new InvalidRequestException("Amount must have at most 2 decimal places");
        }
    }

    private String nextTransferReferenceId() {
        String date = DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now(ZoneOffset.UTC));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "TX-" + date + "-" + suffix;
    }
}