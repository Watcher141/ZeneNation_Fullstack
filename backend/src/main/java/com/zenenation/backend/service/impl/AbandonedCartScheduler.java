package com.zenenation.backend.service.impl;

import com.zenenation.backend.entity.Cart;
// import com.zenenation.backend.entity.CartItem;
import com.zenenation.backend.repository.CartRepository;
import com.zenenation.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Checks for abandoned carts every 30 minutes.
 * A cart is "abandoned" when it has items and was last updated 30–60 min ago.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AbandonedCartScheduler {

    private final CartRepository cartRepository;
    private final EmailService emailService;

    @Scheduled(fixedDelay = 1_800_000, initialDelay = 300_000)
    @Transactional(readOnly = true)
    public void sendAbandonedCartReminders() {
        LocalDateTime thirtyMinsAgo = LocalDateTime.now().minusMinutes(30);
        LocalDateTime sixtyMinsAgo  = LocalDateTime.now().minusMinutes(60);

        List<Cart> abandonedCarts = cartRepository.findAbandonedCarts(sixtyMinsAgo, thirtyMinsAgo);

        log.info("Abandoned cart check: found {} carts to remind", abandonedCarts.size());

        for (Cart cart : abandonedCarts) {
            if (cart.getUser() == null || cart.getUser().getEmail() == null) continue;
            if (cart.getItems() == null || cart.getItems().isEmpty()) continue;

            double total = cart.getItems().stream()
                    .mapToDouble(item -> {
                        BigDecimal price = item.getProduct().getPrice();
                        BigDecimal discount = item.getProduct().getDiscountPercent();
                        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
                            // price * (1 - discountPercent/100)
                            BigDecimal multiplier = BigDecimal.ONE
                                    .subtract(discount.divide(BigDecimal.valueOf(100)));
                            price = price.multiply(multiplier);
                        }
                        return price.doubleValue() * item.getQuantity();
                    })
                    .sum();

            emailService.sendAbandonedCartEmail(
                    cart.getUser().getEmail(),
                    cart.getUser().getName(),
                    cart.getItems().size(),
                    total
            );
        }
    }
}