package com.zenenation.backend.service.impl;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.zenenation.backend.config.AppProperties;
import com.zenenation.backend.dto.request.PlaceOrderRequest;
import com.zenenation.backend.dto.request.UpdateOrderStatusRequest;
import com.zenenation.backend.dto.response.OrderItemResponse;
import com.zenenation.backend.dto.response.OrderResponse;
import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.dto.response.PaymentResponse;
import com.zenenation.backend.entity.*;
import com.zenenation.backend.enums.OrderStatus;
import com.zenenation.backend.enums.PaymentMethod;
import com.zenenation.backend.enums.PaymentStatus;
import com.zenenation.backend.exception.BadRequestException;
import com.zenenation.backend.exception.PaymentException;
import com.zenenation.backend.exception.ResourceNotFoundException;
import com.zenenation.backend.repository.*;
import com.zenenation.backend.service.CouponService;
import com.zenenation.backend.service.EmailService;
import com.zenenation.backend.service.OrderService;
import com.zenenation.backend.service.RewardService;
import com.zenenation.backend.service.ShippingConfigService;
import com.zenenation.backend.util.OrderNumberUtil;
import com.zenenation.backend.util.PriceUtil;
import com.zenenation.backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final SecurityUtil securityUtil;
    private final CouponService couponService;
    private final RewardService rewardService;
    private final ShippingConfigService shippingConfigService;
    private final AppProperties appProperties;
    private final EmailService emailService;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency}")
    private String currency;

    // -------------------------------------------------------------------------
    // PLACE ORDER
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        User user = securityUtil.getCurrentUser();

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        List<CartItem> cartItems = cartItemRepository
                .findAll().stream()
                .filter(item -> item.getCart().getId().equals(cart.getId()))
                .collect(Collectors.toList());

        if (cartItems.isEmpty()) {
            throw new BadRequestException("Your cart is empty. Add items before placing an order.");
        }

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", request.getAddressId()));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Address does not belong to your account");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            if (product.getIsDeleted() || !product.getIsActive()) {
                throw new BadRequestException(
                        "Product '" + product.getName() + "' is no longer available"
                );
            }
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new BadRequestException(
                        "Insufficient stock for '" + product.getName() +
                        "'. Available: " + product.getStockQuantity()
                );
            }
            BigDecimal discountedPrice = PriceUtil.calculateDiscountedPrice(
                    product.getPrice(), product.getDiscountPercent()
            );
            subtotal = subtotal.add(
                    PriceUtil.calculateLineTotal(discountedPrice, item.getQuantity())
            );
        }

        int totalWeightGrams = 0;
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            totalWeightGrams += (product.getWeightGrams() != null ? product.getWeightGrams() : 0) * item.getQuantity();
        }

        var deliverySlabs = shippingConfigService.getDeliverySlabs();
        BigDecimal deliveryCharge = PriceUtil.calculateDeliveryCharge(totalWeightGrams, deliverySlabs);

        BigDecimal codCharge = BigDecimal.ZERO;
        if (request.getPaymentMethod() == PaymentMethod.COD) {
            var codSlabs = shippingConfigService.getCodSlabs();
            codCharge = PriceUtil.calculateCodCharge(subtotal, codSlabs);
        }

        BigDecimal totalAmount = PriceUtil.calculateOrderTotal(
                subtotal, deliveryCharge, codCharge, BigDecimal.ZERO
        );

        if (request.getPaymentMethod() == PaymentMethod.COD) {
            BigDecimal codLimit = BigDecimal.valueOf(appProperties.getOrder().getCodMaxAmount());
            if (totalAmount.compareTo(codLimit) > 0) {
                throw new BadRequestException(
                        String.format("COD is not available for orders above ₹%s. " +
                                "Please use online payment.", codLimit)
                );
            }
        }

        Coupon appliedCoupon = null;
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            appliedCoupon = couponService.validateAndGetCoupon(
                    request.getCouponCode(), subtotal, user.getId()
            );
            discountAmount = couponService.calculateDiscount(appliedCoupon, subtotal);
            totalAmount = PriceUtil.calculateOrderTotal(subtotal, deliveryCharge, codCharge, discountAmount);
        }

        int rewardPointsToRedeem = (request.getRedeemPoints() != null) ? request.getRedeemPoints() : 0;
        BigDecimal rewardDiscountAmount = BigDecimal.ZERO;
        if (rewardPointsToRedeem > 0) {
            int maxAllowed = rewardService.getMaxRedeemablePoints(user.getId(), subtotal.doubleValue());
            if (rewardPointsToRedeem > maxAllowed) {
                throw new BadRequestException(
                        "Cannot redeem more than " + maxAllowed + " reward points for this order"
                );
            }
            int discountRupees = rewardPointsToRedeem / 2;
            rewardDiscountAmount = BigDecimal.valueOf(discountRupees);
            discountAmount = discountAmount.add(rewardDiscountAmount);
            totalAmount = PriceUtil.calculateOrderTotal(subtotal, deliveryCharge, codCharge, discountAmount);
            
            if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
                totalAmount = BigDecimal.ZERO;
            }
        }

        Order order = Order.builder()
                .orderNumber(OrderNumberUtil.generate())
                .user(user)
                .subtotal(subtotal)
                .deliveryCharge(deliveryCharge)
                .codCharge(codCharge)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .deliveryName(address.getName())
                .deliveryPhone(address.getPhoneNumber())
                .deliveryAddressLine1(address.getAddressLine1())
                .deliveryAddressLine2(address.getAddressLine2())
                .deliveryCity(address.getCity())
                .deliveryState(address.getState())
                .deliveryPincode(address.getPincode())
                .deliveryCountry(address.getCountry())
                .userNote(request.getUserNote())
                .build();

        order = orderRepository.save(order);

        if (rewardPointsToRedeem > 0) {
            rewardService.redeemPoints(user.getId(), rewardPointsToRedeem, order);
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            BigDecimal discountedPrice = PriceUtil.calculateDiscountedPrice(
                    product.getPrice(), product.getDiscountPercent()
            );
            BigDecimal lineTotal = PriceUtil.calculateLineTotal(
                    discountedPrice, cartItem.getQuantity()
            );

            String imageUrl = productImageRepository
                    .findByProductIdAndIsPrimaryTrue(product.getId())
                    .map(ProductImage::getImageUrl)
                    .orElse(null);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .productImageUrl(imageUrl)
                    .priceAtPurchase(discountedPrice)
                    .quantity(cartItem.getQuantity())
                    .totalPrice(lineTotal)
                    .build();

            orderItems.add(orderItemRepository.save(orderItem));

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        Payment payment = Payment.builder()
                .order(order)
                .amount(totalAmount)
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .build();

        String razorpayOrderId = null;

        if (request.getPaymentMethod() == PaymentMethod.ONLINE) {
            razorpayOrderId = createRazorpayOrder(order.getId(), totalAmount);
            payment.setRazorpayOrderId(razorpayOrderId);
        }

        paymentRepository.save(payment);

        cartItemRepository.deleteByCartId(cart.getId());

        log.info("Order placed: orderId={}, orderNumber={}, userId={}, method={}",
                order.getId(), order.getOrderNumber(), user.getId(), request.getPaymentMethod());

        // ── SEND EMAILS ASYNC (ONLY FOR COD) ──────
        if (request.getPaymentMethod() == PaymentMethod.COD) {
            final String finalTotalAmount = totalAmount.toPlainString();
            final Order finalOrder = order;
            final String customerEmail = user.getEmail();
            final String customerName = user.getName();
            final String paymentMethodStr = request.getPaymentMethod().name();
            
            CompletableFuture.runAsync(() -> {
                try {
                    // 1. Send to Customer
                    log.info("Attempting to send COD order confirmation to customer: {}", customerEmail);
                    emailService.sendOrderConfirmationEmail(
                            customerEmail,
                            finalOrder.getOrderNumber(),
                            finalTotalAmount
                    );
                } catch (Exception e) {
                    log.error("Failed to send customer confirmation email: {}", e.getMessage(), e);
                }

                try {
                    // 2. Send to Admin
                    log.info("Attempting to send new COD order admin notification to zenenationstore@gmail.com");
                    emailService.sendNewOrderAdminEmail(
                            "zenenationstore@gmail.com",
                            finalOrder.getOrderNumber(),
                            customerName,
                            finalTotalAmount,
                            paymentMethodStr
                    );
                } catch (Exception e) {
                    log.error("Failed to send admin notification email: {}", e.getMessage(), e);
                }
            });
        }

        OrderResponse response = toOrderResponse(order, orderItems, payment, user);
        response.setRazorpayOrderId(razorpayOrderId);
        return response;
    }

    // -------------------------------------------------------------------------
    // USER — VIEW ORDERS
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getMyOrders(int page, int size) {
        User user = securityUtil.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderRepository.findByUserId(user.getId(), pageable);
        return PagedResponse.of(orders.map(o -> toOrderResponse(o, null, null, user)));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderById(Long orderId) {
        User user = securityUtil.getCurrentUser();
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        return toOrderResponse(order, items, payment, user);
    }

    // -------------------------------------------------------------------------
    // USER — CANCEL ORDER
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        User user = securityUtil.getCurrentUser();
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException(
                    "This order cannot be cancelled because it has already been " +
                    order.getStatus().name().toLowerCase() + ". " +
                    "Once an order is shipped, cancellation is not possible."
            );
        }

        order.setStatus(OrderStatus.CANCELLED);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            if (item.getProduct() != null) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }

        order = orderRepository.save(order);
        log.info("Order cancelled: orderId={}, userId={}", orderId, user.getId());

        // ── SEND CANCELLATION EMAILS ASYNC ──────
        final Order finalOrder = order;
        final String customerEmail = user.getEmail();
        final String customerName = user.getName();
        
        CompletableFuture.runAsync(() -> {
            try {
                // 1. Notify Admin
                log.info("Attempting to send admin cancellation email to zenenationstore@gmail.com");
                emailService.sendOrderCancellationAdminEmail(
                        "zenenationstore@gmail.com",
                        finalOrder.getOrderNumber(),
                        customerName,
                        finalOrder.getTotalAmount().toPlainString()
                );
            } catch (Exception e) {
                log.error("Failed to send admin cancellation email: {}", e.getMessage(), e);
            }

            try {
                // 2. Notify Customer
                log.info("Attempting to send customer cancellation email to {}", customerEmail);
                emailService.sendOrderCancellationEmail(
                        customerEmail,
                        customerName,
                        finalOrder.getOrderNumber()
                    );
            } catch (Exception e) {
                log.error("Failed to send customer cancellation email: {}", e.getMessage(), e);
            }
        });

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        return toOrderResponse(order, items, payment, user);
    }

    // -------------------------------------------------------------------------
    // ADMIN — VIEW ALL ORDERS
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrders(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders;

        if (status != null && !status.isBlank()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                orders = orderRepository.findByStatus(orderStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid order status: " + status);
            }
        } else {
            orders = orderRepository.findAll(pageable);
        }

        return PagedResponse.of(orders.map(o -> toOrderResponse(o, null, null, o.getUser())));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdForAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        return toOrderResponse(order, items, payment, order.getUser());
    }

    // -------------------------------------------------------------------------
    // ADMIN — UPDATE ORDER STATUS
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderStatus newStatus = request.getStatus();
        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == OrderStatus.CANCELLED || currentStatus == OrderStatus.DELIVERED) {
            throw new BadRequestException(
                    "Cannot update status of a " + currentStatus + " order"
            );
        }

        if (newStatus == OrderStatus.DELIVERED
                && order.getPaymentMethod() == PaymentMethod.COD) {
            Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
            if (payment != null) {
                payment.setStatus(PaymentStatus.PAID);
                paymentRepository.save(payment);
            }
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        if (newStatus == OrderStatus.DELIVERED) {
            rewardService.creditOrderRewards(order);
        }
        if (newStatus == OrderStatus.CANCELLED) {
            rewardService.refundRedeemedPoints(order);
        }

        order.setStatus(newStatus);
        if (request.getAdminNote() != null) {
            order.setAdminNote(request.getAdminNote());
        }

        order = orderRepository.save(order);
        log.info("Order status updated: orderId={}, status={}", orderId, newStatus);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        return toOrderResponse(order, items, payment, order.getUser());
    }

    // -------------------------------------------------------------------------
    // RAZORPAY
    // -------------------------------------------------------------------------

    private String createRazorpayOrder(Long ourOrderId, BigDecimal amount) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue());
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", "order_" + ourOrderId);

            com.razorpay.Order razorpayOrder = client.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            log.info("Razorpay order created: razorpayOrderId={}, ourOrderId={}",
                    razorpayOrderId, ourOrderId);
            return razorpayOrderId;

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage());
            throw new PaymentException("Failed to initialize payment. Please try again.");
        }
    }

    // -------------------------------------------------------------------------
    // MAPPER
    // -------------------------------------------------------------------------

    private OrderResponse toOrderResponse(Order order, List<OrderItem> items,
                                           Payment payment, User user) {
        List<OrderItem> orderItems = items != null
                ? items
                : orderItemRepository.findByOrderId(order.getId());

        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProductName())
                        .productImageUrl(item.getProductImageUrl())
                        .priceAtPurchase(item.getPriceAtPurchase())
                        .quantity(item.getQuantity())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        PaymentResponse paymentResponse = null;
        if (payment != null) {
            paymentResponse = PaymentResponse.builder()
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

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .subtotal(order.getSubtotal())
                .deliveryCharge(order.getDeliveryCharge())
                .codCharge(order.getCodCharge())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .orderItems(itemResponses)
                .deliveryName(order.getDeliveryName())
                .deliveryPhone(order.getDeliveryPhone())
                .deliveryAddressLine1(order.getDeliveryAddressLine1())
                .deliveryAddressLine2(order.getDeliveryAddressLine2())
                .deliveryCity(order.getDeliveryCity())
                .deliveryState(order.getDeliveryState())
                .deliveryPincode(order.getDeliveryPincode())
                .deliveryCountry(order.getDeliveryCountry())
                .payment(paymentResponse)
                .userEmail(user != null ? user.getEmail() : null)
                .userName(user != null ? user.getName() : null)
                .userNote(order.getUserNote())
                .adminNote(order.getAdminNote())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}