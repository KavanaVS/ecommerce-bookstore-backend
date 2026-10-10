package com.bookstore.service;

import com.bookstore.dto.AddressRequest;
import com.bookstore.dto.AddressResponse;
import com.bookstore.dto.CheckoutRequest;
import com.bookstore.dto.OrderItemResponse;
import com.bookstore.dto.OrderResponse;
import com.bookstore.dto.OrderStatusUpdateRequest;
import com.bookstore.exception.BookNotFoundException;
import com.bookstore.exception.EmptyCartException;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.InvalidCredentialsException;
import com.bookstore.exception.OrderNotFoundException;
import com.bookstore.model.Address;
import com.bookstore.model.Book;
import com.bookstore.model.Cart;
import com.bookstore.model.CartItem;
import com.bookstore.model.Order;
import com.bookstore.model.OrderItem;
import com.bookstore.model.OrderStatus;
import com.bookstore.model.Payment;
import com.bookstore.model.PaymentMethod;
import com.bookstore.model.PaymentStatus;
import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartItemRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.OrderItemRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.PaymentRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                         PaymentRepository paymentRepository, CartRepository cartRepository,
                         CartItemRepository cartItemRepository, BookRepository bookRepository,
                         UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EmptyCartException("Cart is empty"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new EmptyCartException("Cart is empty");
        }

        List<Book> books = cartItems.stream()
                .map(item -> findBookOrThrow(item.getBookId()))
                .toList();

        for (int i = 0; i < cartItems.size(); i++) {
            CartItem item = cartItems.get(i);
            Book book = books.get(i);
            if (item.getQuantity() > book.getStockQuantity()) {
                throw new InsufficientStockException("Insufficient stock for book: " + book.getTitle());
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem item = cartItems.get(i);
            Book book = books.get(i);
            total = total.add(book.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        Order order = Order.builder()
                .userId(user.getId())
                .status(OrderStatus.PLACED)
                .totalAmount(total)
                .shippingAddress(toAddress(request.getShippingAddress()))
                .billingAddress(toAddress(request.getBillingAddress()))
                .build();
        order = orderRepository.save(order);

        for (int i = 0; i < cartItems.size(); i++) {
            CartItem item = cartItems.get(i);
            Book book = books.get(i);

            OrderItem orderItem = OrderItem.builder()
                    .orderId(order.getId())
                    .bookId(book.getId())
                    .quantity(item.getQuantity())
                    .unitPrice(book.getPrice())
                    .build();
            orderItemRepository.save(orderItem);

            book.setStockQuantity(book.getStockQuantity() - item.getQuantity());
            bookRepository.save(book);
        }

        PaymentMethod paymentMethod = request.getPaymentMethod();
        PaymentStatus paymentStatus = paymentMethod == PaymentMethod.COD ? PaymentStatus.PENDING : PaymentStatus.PAID;

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .amount(total)
                .status(paymentStatus)
                .paymentMethod(paymentMethod)
                .build();
        paymentRepository.save(payment);

        cartItemRepository.deleteAll(cartItems);

        return toOrderResponse(order);
    }

    public List<OrderResponse> getOrders() {
        User user = getCurrentUser();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    public OrderResponse getOrderById(Long id) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));

        if (!order.getUserId().equals(user.getId())) {
            throw new OrderNotFoundException("Order not found with id: " + id);
        }

        return toOrderResponse(order);
    }

    public List<OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toOrderResponse)
                .toList();
    }

    public OrderResponse updateStatus(Long id, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));

        order.setStatus(request.getStatus());
        orderRepository.save(order);

        return toOrderResponse(order);
    }

    private Book findBookOrThrow(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + bookId));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        List<OrderItemResponse> itemResponses = items.stream()
                .map(this::toOrderItemResponse)
                .toList();

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);

        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .paymentStatus(payment == null ? null : payment.getStatus())
                .paymentMethod(payment == null ? null : payment.getPaymentMethod())
                .shippingAddress(toAddressResponse(order.getShippingAddress()))
                .billingAddress(toAddressResponse(order.getBillingAddress()))
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private Address toAddress(AddressRequest request) {
        return Address.builder()
                .line1(request.getLine1())
                .line2(request.getLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .build();
    }

    private AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        return AddressResponse.builder()
                .line1(address.getLine1())
                .line2(address.getLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .build();
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        Book book = findBookOrThrow(item.getBookId());
        BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

        return OrderItemResponse.builder()
                .id(item.getId())
                .bookId(book.getId())
                .title(book.getTitle())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(lineTotal)
                .build();
    }
}
