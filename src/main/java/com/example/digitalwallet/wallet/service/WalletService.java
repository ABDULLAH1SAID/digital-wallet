package com.example.digitalwallet.wallet.service;

import com.example.digitalwallet.common.exception.WalletAlreadyExistsException;
import com.example.digitalwallet.user.entity.User;
import com.example.digitalwallet.user.service.UserService;
import com.example.digitalwallet.wallet.dto.WalletResponse;
import com.example.digitalwallet.wallet.entity.Wallet;
import com.example.digitalwallet.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserService userService;

    public WalletResponse createWallet(Long userId) {

        User user = userService.getUserById(userId);

        if (walletRepository.existsByUserId(userId)) {
            throw new WalletAlreadyExistsException(userId);
        }

        Wallet wallet = new Wallet(user);
        Wallet savedWallet = walletRepository.save(wallet);

        return WalletResponse.from(savedWallet);
    }
}