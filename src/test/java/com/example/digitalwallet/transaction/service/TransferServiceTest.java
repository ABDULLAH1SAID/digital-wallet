package com.example.digitalwallet.transaction.service;

import com.example.digitalwallet.common.exception.InsufficientBalanceException;
import com.example.digitalwallet.common.exception.InvalidRequestException;
import com.example.digitalwallet.common.exception.WalletNotFoundException;
import com.example.digitalwallet.transaction.dto.TransferRequest;
import com.example.digitalwallet.transaction.dto.TransferResult;
import com.example.digitalwallet.transaction.entity.Transaction;
import com.example.digitalwallet.transaction.entity.TransactionOperation;
import com.example.digitalwallet.transaction.entity.TransactionStatus;
import com.example.digitalwallet.transaction.entity.TransactionType;
import com.example.digitalwallet.transaction.repository.TransactionRepository;
import com.example.digitalwallet.user.entity.User;
import com.example.digitalwallet.wallet.entity.Wallet;
import com.example.digitalwallet.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private org.springframework.transaction.TransactionStatus transactionStatus;

    @InjectMocks
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    }

    @Test
    void transfer_debitsSenderCreditsReceiverAndWritesBothLegs() {
        Wallet sender = wallet(1L, 1L, "50.00");
        Wallet receiver = wallet(2L, 2L, "5.00");
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(transactionRepository.findByWalletIdAndIdempotencyKeyAndOperation(
                1L, "tx-1", TransactionOperation.TRANSFER
        )).thenReturn(Optional.empty());
        when(walletRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));

        AtomicLong ids = new AtomicLong(20);
        when(transactionRepository.saveAndFlush(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            ReflectionTestUtils.setField(transaction, "id", ids.getAndIncrement());
            return transaction;
        });

        TransferResult result = transferService.transfer(
                1L, "tx-1", new TransferRequest(2L, new BigDecimal("12.00"))
        );

        assertThat(result.created()).isTrue();
        assertThat(result.transfer().status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.transfer().debit().type()).isEqualTo(TransactionType.DEBIT);
        assertThat(result.transfer().credit().type()).isEqualTo(TransactionType.CREDIT);
        assertThat(result.transfer().debit().referenceId()).isEqualTo(result.transfer().credit().referenceId());
        assertThat(sender.getBalance()).isEqualByComparingTo("38.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("17.00");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).saveAndFlush(captor.capture());
        Transaction debit = captor.getAllValues().get(0);
        Transaction credit = captor.getAllValues().get(1);
        assertThat(debit.getOperation()).isEqualTo(TransactionOperation.TRANSFER);
        assertThat(debit.getIdempotencyKey()).isEqualTo("tx-1");
        assertThat(credit.getIdempotencyKey()).isNull();
        assertThat(credit.getOperation()).isEqualTo(TransactionOperation.TRANSFER);
    }

    @Test
    void transfer_sameKey_replaysWithoutMovingMoneyAgain() {
        Wallet sender = wallet(1L, 1L, "50.00");
        Wallet receiver = wallet(2L, 2L, "5.00");
        Transaction debit = leg(30L, sender, receiver, TransactionType.DEBIT, "12.00", "38.00", "TX-1");
        debit.setOperation(TransactionOperation.TRANSFER);
        debit.setIdempotencyKey("tx-1");
        Transaction credit = leg(31L, receiver, sender, TransactionType.CREDIT, "12.00", "17.00", "TX-1");

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(transactionRepository.findByWalletIdAndIdempotencyKeyAndOperation(
                1L, "tx-1", TransactionOperation.TRANSFER
        )).thenReturn(Optional.of(debit));
        when(transactionRepository.findByReferenceId("TX-1")).thenReturn(List.of(debit, credit));

        TransferResult result = transferService.transfer(
                1L, "tx-1", new TransferRequest(2L, new BigDecimal("12.00"))
        );

        assertThat(result.created()).isFalse();
        assertThat(result.transfer().referenceId()).isEqualTo("TX-1");
        assertThat(sender.getBalance()).isEqualByComparingTo("50.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("5.00");
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void transfer_sameKeyUsedEarlierForDeposit_stillExecutesTransfer() {
        Wallet sender = wallet(1L, 1L, "50.00");
        Wallet receiver = wallet(2L, 2L, "0.00");
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(transactionRepository.findByWalletIdAndIdempotencyKeyAndOperation(
                1L, "shared", TransactionOperation.TRANSFER
        )).thenReturn(Optional.empty());
        when(walletRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));
        when(transactionRepository.saveAndFlush(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            ReflectionTestUtils.setField(transaction, "id", 40L);
            return transaction;
        });

        TransferResult result = transferService.transfer(
                1L, "shared", new TransferRequest(2L, new BigDecimal("5.00"))
        );

        assertThat(result.created()).isTrue();
        assertThat(sender.getBalance()).isEqualByComparingTo("45.00");
    }

    @Test
    void transfer_toSameWallet_throws() {
        Wallet sender = wallet(1L, 1L, "50.00");
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(transactionRepository.findByWalletIdAndIdempotencyKeyAndOperation(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(walletRepository.findById(1L)).thenReturn(Optional.of(sender));

        assertThatThrownBy(() -> transferService.transfer(
                1L, "tx-1", new TransferRequest(1L, new BigDecimal("5.00"))
        )).isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("same wallet");
    }

    @Test
    void transfer_insufficientBalance_throwsAndDoesNotWriteLedger() {
        Wallet sender = wallet(1L, 1L, "5.00");
        Wallet receiver = wallet(2L, 2L, "0.00");
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(transactionRepository.findByWalletIdAndIdempotencyKeyAndOperation(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(walletRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(walletRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(walletRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));

        assertThatThrownBy(() -> transferService.transfer(
                1L, "tx-1", new TransferRequest(2L, new BigDecimal("10.00"))
        )).isInstanceOf(InsufficientBalanceException.class);

        verify(transactionRepository, never()).saveAndFlush(any());
        assertThat(sender.getBalance()).isEqualByComparingTo("5.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void transfer_senderWalletMissing_throws() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(
                1L, "tx-1", new TransferRequest(2L, new BigDecimal("1.00"))
        )).isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void transfer_receiverWalletMissing_throws() {
        Wallet sender = wallet(1L, 1L, "50.00");
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(sender));
        when(transactionRepository.findByWalletIdAndIdempotencyKeyAndOperation(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(walletRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(
                1L, "tx-1", new TransferRequest(99L, new BigDecimal("1.00"))
        )).isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void transfer_blankKey_throws() {
        assertThatThrownBy(() -> transferService.transfer(
                1L, " ", new TransferRequest(2L, new BigDecimal("1.00"))
        )).isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Idempotency-Key");
    }

    private Wallet wallet(Long walletId, Long userId, String balance) {
        User user = new User();
        user.setId(userId);
        user.setUsername("user" + userId);
        user.setEmail("user" + userId + "@example.com");
        Wallet wallet = new Wallet(user);
        ReflectionTestUtils.setField(wallet, "id", walletId);
        ReflectionTestUtils.setField(wallet, "balance", new BigDecimal(balance));
        return wallet;
    }

    private Transaction leg(
            Long id,
            Wallet wallet,
            Wallet counterparty,
            TransactionType type,
            String amount,
            String balanceAfter,
            String referenceId
    ) {
        Transaction transaction = new Transaction();
        ReflectionTestUtils.setField(transaction, "id", id);
        transaction.setWallet(wallet);
        transaction.setCounterpartyWallet(counterparty);
        transaction.setType(type);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setBalanceAfter(new BigDecimal(balanceAfter));
        transaction.setReferenceId(referenceId);
        transaction.setStatus(TransactionStatus.COMPLETED);
        return transaction;
    }
}