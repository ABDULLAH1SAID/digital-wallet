package com.example.digitalwallet.transaction.service;

import com.example.digitalwallet.common.exception.InvalidFilterException;
import com.example.digitalwallet.common.exception.InvalidPaginationException;
import com.example.digitalwallet.common.exception.WalletNotFoundException;
import com.example.digitalwallet.transaction.dto.TransactionPageResponse;
import com.example.digitalwallet.transaction.entity.Transaction;
import com.example.digitalwallet.transaction.entity.TransactionType;
import com.example.digitalwallet.transaction.repository.TransactionRepository;
import com.example.digitalwallet.wallet.entity.Wallet;
import com.example.digitalwallet.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final int MIN_PAGE = 0;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("createdAt", "amount");

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public TransactionPageResponse getTransactions(
            Long userId,
            Long walletId,
            int page,
            int size,
            String type,
            String sort
    ) {
        Wallet wallet = walletRepository.findById(walletId)
                .filter(w -> w.getUser().getId().equals(userId))
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        validatePagination(page, size);
        TransactionType transactionType = parseType(type);
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        Page<Transaction> result = transactionRepository.findByWalletIdAndType(
                wallet.getId(),
                transactionType,
                pageable
        );

        return TransactionPageResponse.from(result);
    }

    private void validatePagination(int page, int size) {
        if (page < MIN_PAGE) {
            throw new InvalidPaginationException("page must be greater than or equal to 0");
        }
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new InvalidPaginationException("size must be between 1 and 100");
        }
    }

    private TransactionType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return TransactionType.valueOf(type.trim());
        } catch (IllegalArgumentException ex) {
            throw new InvalidFilterException("type must be CREDIT or DEBIT");
        }
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(",");
        if (parts.length == 0 || parts.length > 2) {
            throw new InvalidPaginationException("sort must be in the form property,direction");
        }

        String property = parts[0].trim();
        if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
            throw new InvalidPaginationException("sort property must be createdAt or amount");
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length == 2) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException ex) {
                throw new InvalidPaginationException("sort direction must be asc or desc");
            }
        }

        return Sort.by(direction, property);
    }
}
