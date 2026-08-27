package com.example.digitalwallet.transaction.service;

import com.example.digitalwallet.common.exception.InvalidFilterException;
import com.example.digitalwallet.common.exception.InvalidPaginationException;
import com.example.digitalwallet.common.exception.WalletNotFoundException;
import com.example.digitalwallet.transaction.dto.TransactionPageResponse;
import com.example.digitalwallet.transaction.entity.Transaction;
import com.example.digitalwallet.transaction.entity.TransactionStatus;
import com.example.digitalwallet.transaction.entity.TransactionType;
import com.example.digitalwallet.transaction.repository.TransactionRepository;
import com.example.digitalwallet.user.entity.User;
import com.example.digitalwallet.wallet.entity.Wallet;
import com.example.digitalwallet.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void getTransactions_returnsPageForOwner() {
        Wallet wallet = wallet(5L, 1L);
        Transaction transaction = transaction(wallet);
        when(walletRepository.findById(5L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletIdAndType(eq(5L), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction), PageRequest.of(0, 20), 1));

        TransactionPageResponse response = transactionService.getTransactions(
                1L, 5L, 0, 20, null, "createdAt,desc"
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().get(0).type()).isEqualTo(TransactionType.CREDIT);
    }

    @Test
    void getTransactions_filtersByCredit() {
        Wallet wallet = wallet(5L, 1L);
        when(walletRepository.findById(5L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletIdAndType(eq(5L), eq(TransactionType.CREDIT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        transactionService.getTransactions(1L, 5L, 0, 20, "CREDIT", "createdAt,desc");

        verify(transactionRepository).findByWalletIdAndType(eq(5L), eq(TransactionType.CREDIT), any(Pageable.class));
    }

    @Test
    void getTransactions_sortsByAmountAscending() {
        Wallet wallet = wallet(5L, 1L);
        when(walletRepository.findById(5L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletIdAndType(eq(5L), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        transactionService.getTransactions(1L, 5L, 0, 10, null, "amount,asc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findByWalletIdAndType(eq(5L), eq(null), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getSort().getOrderFor("amount").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getTransactions_whenNotOwner_throwsNotFound() {
        when(walletRepository.findById(5L)).thenReturn(Optional.of(wallet(5L, 1L)));

        assertThatThrownBy(() -> transactionService.getTransactions(2L, 5L, 0, 20, null, "createdAt,desc"))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void getTransactions_invalidType_throws() {
        when(walletRepository.findById(5L)).thenReturn(Optional.of(wallet(5L, 1L)));

        assertThatThrownBy(() -> transactionService.getTransactions(1L, 5L, 0, 20, "REFUND", "createdAt,desc"))
                .isInstanceOf(InvalidFilterException.class);
    }

    @Test
    void getTransactions_invalidPage_throws() {
        when(walletRepository.findById(5L)).thenReturn(Optional.of(wallet(5L, 1L)));

        assertThatThrownBy(() -> transactionService.getTransactions(1L, 5L, -1, 20, null, "createdAt,desc"))
                .isInstanceOf(InvalidPaginationException.class);
    }

    @Test
    void getTransactions_sizeAboveMax_throws() {
        when(walletRepository.findById(5L)).thenReturn(Optional.of(wallet(5L, 1L)));

        assertThatThrownBy(() -> transactionService.getTransactions(1L, 5L, 0, 101, null, "createdAt,desc"))
                .isInstanceOf(InvalidPaginationException.class);
    }

    @Test
    void getTransactions_invalidSortProperty_throws() {
        when(walletRepository.findById(5L)).thenReturn(Optional.of(wallet(5L, 1L)));

        assertThatThrownBy(() -> transactionService.getTransactions(1L, 5L, 0, 20, null, "status,desc"))
                .isInstanceOf(InvalidPaginationException.class);
    }

    private Wallet wallet(Long walletId, Long userId) {
        User user = new User();
        user.setId(userId);
        user.setUsername("user" + userId);
        user.setEmail("user" + userId + "@example.com");
        Wallet wallet = new Wallet(user);
        ReflectionTestUtils.setField(wallet, "id", walletId);
        return wallet;
    }

    private Transaction transaction(Wallet wallet) {
        Transaction transaction = new Transaction();
        ReflectionTestUtils.setField(transaction, "id", 9L);
        transaction.setWallet(wallet);
        transaction.setType(TransactionType.CREDIT);
        transaction.setAmount(new BigDecimal("10.00"));
        transaction.setBalanceAfter(new BigDecimal("10.00"));
        transaction.setReferenceId("DEP-1");
        transaction.setStatus(TransactionStatus.COMPLETED);
        return transaction;
    }
}