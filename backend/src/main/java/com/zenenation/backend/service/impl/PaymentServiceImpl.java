package com.zenenation.backend.service.impl;

import com.zenenation.backend.dto.request.PaymentVerificationRequest;
import com.zenenation.backend.dto.response.PaymentResponse;
import com.zenenation.backend.entity.Order;
import com.zenenation.backend.entity.Payment;
import com.zenenation.backend.enums.OrderStatus;
import com.zenenation.backend.enums.PaymentStatus;
import com.zenenation.backend.exception.PaymentException;
import com.zenenation.backend.exception.ResourceNotFoundException;
import com.zenenation.backend.repository.OrderRepository;
import com.zenenation.backend.repository.PaymentRepository;
import com.zenenation.backend.service.PaymentService;
import com.zenenation.backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * RAZORPAY PAYMENT VERIFICATION FLOW:
 *
 * After user pays on Razorpay checkout modal, Razorpay sends:
 *   - razorpay_order_id   (the ID we created in OrderService)
 *   - razorpay_payment_id (new ID from Razorpay after payment)
 *   - razorpay_signature  (HMAC-SHA256 signature to verify)
 *
 * We verify by recomputing the signature ourselves:
 *   expectedSignature = HMAC_SHA256(
 *       key    = razorpayKeySecret,
 *       data   = razorpayOrderId + "|" + razorpayPaymentId
 *   )
 *
 * If expectedSignature == receivedSignature → payment is authentic
 * If not → payment was tampered with or forged → reject
 *
 * This prevents anyone from faking a payment by sending false data to our API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SecurityUtil securityUtil;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    // -------------------------------------------------------------------------
    // VERIFY PAYMENT
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public PaymentResponse verifyPayment(PaymentVerificationRequest request) {
        // Load the payment record created during order placement
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for Razorpay order: " + request.getRazorpayOrderId()
                ));

        Order order = payment.getOrder();

        // Security: verify this order belongs to the current user
        Long currentUserId = securityUtil.getCurrentUserId();
        if (!order.getUser().getId().equals(currentUserId)) {
            throw new PaymentException("Payment verification not authorized");
        }

        // Prevent re-verification of already verified payments
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.warn("Payment already verified: razorpayOrderId={}", request.getRazorpayOrderId());
            return toPaymentResponse(payment);
        }

        // ── Verify Razorpay signature ─────────────────────────────────────
        boolean isValid = verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (isValid) {
            // Payment verified — mark as PAID and confirm order
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setRazorpaySignature(request.getRazorpaySignature());
            payment.setStatus(PaymentStatus.PAID);
            paymentRepository.save(payment);

            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);

            log.info("Payment verified successfully: orderId={}, razorpayPaymentId={}",
                    order.getId(), request.getRazorpayPaymentId());
        } else {
            // Invalid signature — mark as FAILED
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment signature verification failed");
            paymentRepository.save(payment);

            order.setStatus(OrderStatus.PAYMENT_FAILED);
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);

            log.warn("Payment signature verification FAILED: razorpayOrderId={}",
                    request.getRazorpayOrderId());

            throw new PaymentException(
                    "Payment verification failed. If money was deducted, it will be refunded within 5-7 business days."
            );
        }

        return toPaymentResponse(payment);
    }

    // -------------------------------------------------------------------------
    // GET PAYMENT BY ORDER
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Long currentUserId = securityUtil.getCurrentUserId();

        Order order = orderRepository.findByIdAndUserId(orderId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for order: " + orderId
                ));

        return toPaymentResponse(payment);
    }

    // -------------------------------------------------------------------------
    // SIGNATURE VERIFICATION
    // -------------------------------------------------------------------------

    /**
     * Verify Razorpay payment signature using HMAC-SHA256.
     *
     * Algorithm:
     *   1. Create data string: razorpayOrderId + "|" + razorpayPaymentId
     *   2. Compute HMAC-SHA256 of data using our Razorpay key secret
     *   3. Convert result to lowercase hex string
     *   4. Compare with received signature (constant-time comparison)
     *
     * IMPORTANT: Use constant-time comparison (MessageDigest.isEqual or similar)
     * to prevent timing attacks. Regular string .equals() is NOT constant-time.
     */
    private boolean verifySignature(String razorpayOrderId, String razorpayPaymentId,
                                     String receivedSignature) {
        try {
            String data = razorpayOrderId + "|" + razorpayPaymentId;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKey);

            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = bytesToHex(hash);

            // Constant-time comparison to prevent timing attacks
            return constantTimeEquals(expectedSignature, receivedSignature);

        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Convert byte array to lowercase hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Constant-time string comparison.
     * Prevents timing attacks where attacker could guess the signature
     * by measuring how long the comparison takes.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    // -------------------------------------------------------------------------
    // MAPPER
    // -------------------------------------------------------------------------

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
