package com.example.digitalwallet.wallet.service;

import com.example.digitalwallet.common.exception.InvalidRequestException;
import com.example.digitalwallet.common.exception.UserNotFoundException;
import com.example.digitalwallet.common.exception.WalletAlreadyExistsException;
import com.example.digitalwallet.common.exception.WalletNotFoundException;
import com.example.digitalwallet.transaction.entity.Transaction;
import com.example.digitalwallet.transaction.entity.TransactionOperation;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private UserService userService;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private TransactionStatus transactionStatus;

    @InjectMocks
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    }

    @Test
    void createWallet_createsWalletForUser() {
        User user = user(1L);
        when(userService.getUserById(1L)).thenReturn(user);
        when(walletRepository.existsByUserId(1L)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet wallet = invocation.getArgument(0);
            ReflectionTestUtils.setField(wallet, "id", 10L);
            return wallet;
        });

        WalletResponse response = walletService.createWallet(1L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getBalance()).isEqualByComparingTo("0");
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void createWallet_whenAlreadyExists_throwsConflict() {
        when(userService.getUserById(1L)).thenReturn(user(1L));
        when(walletRepository.existsByUserId(1L)).thenReturn(true);

        assertThatThrownBy(() -> walletService.createWallet(1L))
                .isInstanceOf(WalletAlreadyExistsException.class);
        verify(walletRepository, never()).save(any());
    }

    @Test
    void createWallet_whenUserMissing_throws() {
        when(userService.getUserById(99L)).thenThrow(new UserNotFoundException(99L));

        assertThatThrownBy(() -> walletService.createWallet(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getBalance_returnsBalanceForOwner() {
        Wallet wallet = wallet(5L, 1L, "40.00");
        when(walletRepository.findById(5L)).thenReturn(Optional.of(wallet));

        BalanceResponse response = walletService.getBalance(1L, 5L);

        assertThat(response.walletId()).isEqualTo(5L);
        assertThat(response.balance()).isEqualByComparingTo("40.00");
    }

    @Test
    void getBalance_whenNotOwner_throwsNotFound() {
        Wallet wallet = wallet(5L, 1L, "40.00");
        when(walletRepository.findById(5L)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.getBalance(2L, 5L))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void deposit_creditsWalletAndRecordsCompletedCredit() {
        Wallet wallet = wallet(5L, 1L, "10.00");
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletIdAndIdempotencyKeyAndOperation(
                5L, "dep-1", TransactionOperation.DEPOSIT
        )).thenReturn(Optional.empty());
        when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.saveAndFlush(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            ReflectionTestUtils.setField(transaction, "id", 100L);
            return transaction;
        });

        DepositResult result = walletService.deposit(1L, "dep-1", new DepositRequest(new BigDecimal("15.50")));

        assertThat(result.created()).isTrue();
        assertThat(result.transaction().transactionId()).isEqualTo(100L);
        assertThat(result.transaction().type()).isEqualTo(TransactionType.CREDIT);
        assertThat(result.transaction().amount()).isEqualByComparingTo("15.50");
        assertThat(result.transaction().balanceAfter()).isEqualByComparingTo("25.50");
        assertThat(wallet.getBalance()).isEqualByComparingTo("25.50");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getOperation()).isEqualTo(TransactionOperation.DEPOSIT);
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("dep-1");
        assertThat(captor.getValue().getReferenceId()).startsWith("DEP-");
    }

    @Test
    void deposit_sameKey_replaysWithoutCreditingAgain() {
        Wallet wallet = wallet(5L, 1L, "10.00");
        Transaction existing = existingDeposit(wallet, "dep-1", "15.50");
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletIdAndIdempotencyKeyAndOperation(
                5L, "dep-1", TransactionOperation.DEPOSIT
        )).thenReturn(Optional.of(existing));

        DepositResult result = walletService.deposit(1L, "dep-1", new DepositRequest(new BigDecimal("15.50")));

        assertThat(result.created()).isFalse();
        assertThat(result.transaction().transactionId()).isEqualTo(100L);
        assertThat(wallet.getBalance()).isEqualByComparingTo("10.00");
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void deposit_whenConcurrentDuplicate_returnsExisting() {
        Wallet wallet = wallet(5L, 1L, "10.00");
        Transaction existing = existingDeposit(wallet, "dep-1", "15.50");
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletIdAndIdempotencyKeyAndOperation(
                eq(5L), eq("dep-1"), eq(TransactionOperation.DEPOSIT)
        )).thenReturn(Optional.empty(), Optional.of(existing));
        when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.saveAndFlush(any(Transaction.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        DepositResult result = walletService.deposit(1L, "dep-1", new DepositRequest(new BigDecimal("15.50")));

        assertThat(result.created()).isFalse();
        assertThat(result.transaction().transactionId()).isEqualTo(100L);
    }

    @Test
    void deposit_blankKey_throws() {
        assertThatThrownBy(() -> walletService.deposit(1L, "  ", new DepositRequest(new BigDecimal("10.00"))))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Idempotency-Key");
    }

    @Test
    void deposit_nonPositiveAmount_throws() {
        Wallet wallet = wallet(5L, 1L, "10.00");
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletIdAndIdempotencyKeyAndOperation(any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.deposit(1L, "dep-1", new DepositRequest(new BigDecimal("0.00"))))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void deposit_whenWalletMissing_throws() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.deposit(1L, "dep-1", new DepositRequest(new BigDecimal("10.00"))))
                .isInstanceOf(WalletNotFoundException.class);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setEmail("user" + id + "@example.com");
        return user;
    }

    private Wallet wallet(Long walletId, Long userId, String balance) {
        Wallet wallet = new Wallet(user(userId));
        ReflectionTestUtils.setField(wallet, "id", walletId);
        ReflectionTestUtils.setField(wallet, "balance", new BigDecimal(balance));
        return wallet;
    }

    private Transaction existingDeposit(Wallet wallet, String key, String amount) {
        Transaction transaction = new Transaction();
        ReflectionTestUtils.setField(transaction, "id", 100L);
        transaction.setWallet(wallet);
        transaction.setType(TransactionType.CREDIT);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setBalanceAfter(new BigDecimal(amount));
        transaction.setReferenceId("DEP-EXISTING");
        transaction.setOperation(TransactionOperation.DEPOSIT);
        transaction.setIdempotencyKey(key);
        return transaction;
    }
}