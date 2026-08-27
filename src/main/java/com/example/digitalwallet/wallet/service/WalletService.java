package com.example.digitalwallet.wallet.service;

import com.example.digitalwallet.common.exception.InvalidRequestException;
import com.example.digitalwallet.common.exception.WalletAlreadyExistsException;
import com.example.digitalwallet.common.exception.WalletNotFoundException;
import com.example.digitalwallet.transaction.dto.TransactionResponse;
import com.example.digitalwallet.transaction.entity.Transaction;
import com.example.digitalwallet.transaction.entity.TransactionOperation;
import com.example.digitalwallet.transaction.entity.TransactionStatus;
import com.example.digitalwallet.transaction.entity.TransactionType;
import com.example.digitalwallet.transaction.repository.TransactionRepository;
import com.example.digitalwallet.user.entity.User;
import com.example.digitalwallet.user.service.UserService;
import com.example.digitalwallet.wallet.dto.BalanceResponse;
import com.example.digitalwallet.wallet.dto.DepositRequest;
import com.example.digitalwallet.wallet.dto.DepositResult;
import com.example.digitalwallet.wallet.dto.WalletResponse;
import com.example.digitalwallet.wallet.entity.Wallet;
import com.example.digitalwallet.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserService userService;
    private final TransactionRepository transactionRepository;
    private final PlatformTransactionManager transactionManager;

    public WalletResponse createWallet(Long userId) {

        User user = userService.getUserById(userId);

        if (walletRepository.existsByUserId(userId)) {
            throw new WalletAlreadyExistsException(userId);
        }

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        try {
            return txTemplate.execute(status -> {
                Wallet savedWallet = walletRepository.saveAndFlush(new Wallet(user));
                return WalletResponse.from(savedWallet);
            });
        } catch (DataIntegrityViolationException ex) {
            if (walletRepository.existsByUserId(userId)) {
                throw new WalletAlreadyExistsException(userId);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long userId, Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .filter(w -> w.getUser().getId().equals(userId))
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        return new BalanceResponse(wallet.getId(), wallet.getBalance());
    }

    public DepositResult deposit(Long userId, String idempotencyKey, DepositRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidRequestException("Idempotency-Key header is missing");
        }

        String key = idempotencyKey.trim();

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);

        Optional<Transaction> existing = transactionRepository
                .findByWalletIdAndIdempotencyKeyAndOperation(
                        wallet.getId(),
                        key,
                        TransactionOperation.DEPOSIT
                );
        if (existing.isPresent()) {
            return new DepositResult(TransactionResponse.from(existing.get()), false);
        }

        validateAmount(request);

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        try {
            TransactionResponse created = txTemplate.execute(status ->
                    executeDeposit(userId, key, request.amount())
            );
            return new DepositResult(created, true);
        } catch (DataIntegrityViolationException ex) {
            return transactionRepository
                    .findByWalletIdAndIdempotencyKeyAndOperation(
                            wallet.getId(),
                            key,
                            TransactionOperation.DEPOSIT
                    )
                    .map(transaction -> new DepositResult(TransactionResponse.from(transaction), false))
                    .orElseThrow(() -> ex);
        }
    }

    private TransactionResponse executeDeposit(Long userId, String idempotencyKey, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(WalletNotFoundException::new);

        wallet.credit(amount);
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setType(TransactionType.CREDIT);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(wallet.getBalance());
        transaction.setCounterpartyWallet(null);
        transaction.setReferenceId(nextDepositReferenceId());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setOperation(TransactionOperation.DEPOSIT);
        transaction.setIdempotencyKey(idempotencyKey);

        Transaction saved = transactionRepository.saveAndFlush(transaction);
        return TransactionResponse.from(saved);
    }

    private void validateAmount(DepositRequest request) {
        if (request == null || request.amount() == null) {
            throw new InvalidRequestException("Amount is required");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Amount must be greater than zero");
        }
        if (request.amount().scale() > 2) {
            throw new InvalidRequestException("Amount must have at most 2 decimal places");
        }
    }

    private String nextDepositReferenceId() {
        String date = DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now(ZoneOffset.UTC));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "DEP-" + date + "-" + suffix;
    }
}