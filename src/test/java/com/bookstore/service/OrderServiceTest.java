package com.bookstore.service;

import com.bookstore.dto.AddressRequest;
import com.bookstore.dto.CheckoutRequest;
import com.bookstore.dto.OrderResponse;
import com.bookstore.dto.OrderStatusUpdateRequest;
import com.bookstore.exception.EmptyCartException;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.OrderNotFoundException;
import com.bookstore.model.Address;
import com.bookstore.model.Book;
import com.bookstore.model.Cart;
import com.bookstore.model.CartItem;
import com.bookstore.model.Order;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User currentUser;
    private Book book;
    private Cart cart;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).email("customer1@example.com").build();
        book = Book.builder().id(10L).title("Clean Code").price(new BigDecimal("20.00")).stockQuantity(5).build();
        cart = Cart.builder().id(100L).userId(currentUser.getId()).build();

        var authentication = new UsernamePasswordAuthenticationToken(currentUser.getEmail(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        lenient().when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CheckoutRequest validCheckoutRequest(PaymentMethod paymentMethod) {
        AddressRequest address = new AddressRequest();
        address.setLine1("221B Baker Street");
        address.setCity("London");
        address.setState("Greater London");
        address.setPostalCode("NW1 6XE");
        address.setCountry("UK");

        CheckoutRequest request = new CheckoutRequest();
        request.setShippingAddress(address);
        request.setBillingAddress(address);
        request.setPaymentMethod(paymentMethod);
        return request;
    }

    @Test
    void checkout_whenSuccessful_createsOrderDecrementsStockCreatesPaymentAndClearsCart() {
        CartItem item = CartItem.builder().id(1L).cartId(cart.getId()).bookId(book.getId()).quantity(2).build();
        Address address = Address.builder().line1("221B Baker Street").city("London").state("Greater London")
                .postalCode("NW1 6XE").country("UK").build();
        Order savedOrder = Order.builder().id(500L).userId(currentUser.getId()).status(OrderStatus.PLACED)
                .totalAmount(new BigDecimal("40.00")).shippingAddress(address).billingAddress(address).build();

        when(cartRepository.findByUserId(currentUser.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(item));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.findByOrderId(savedOrder.getId())).thenReturn(List.of());
        when(paymentRepository.findByOrderId(savedOrder.getId())).thenReturn(
                Optional.of(Payment.builder().orderId(savedOrder.getId()).amount(new BigDecimal("40.00"))
                        .status(PaymentStatus.PAID).paymentMethod(PaymentMethod.CARD).build()));

        OrderResponse response = orderService.checkout(validCheckoutRequest(PaymentMethod.CARD));

        assertThat(response.getTotalAmount()).isEqualByComparingTo("40.00");
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(response.getShippingAddress().getCity()).isEqualTo("London");
        assertThat(response.getBillingAddress().getCity()).isEqualTo("London");
        verify(orderRepository).save(any(Order.class));
        verify(bookRepository).save(book);
        assertThat(book.getStockQuantity()).isEqualTo(3);
        verify(paymentRepository).save(any(Payment.class));
        verify(cartItemRepository).deleteAll(List.of(item));
    }

    @Test
    void checkout_whenInsufficientStock_throwsExceptionAndPersistsNothing() {
        CartItem item = CartItem.builder().id(1L).cartId(cart.getId()).bookId(book.getId()).quantity(99).build();

        when(cartRepository.findByUserId(currentUser.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(item));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> orderService.checkout(validCheckoutRequest(PaymentMethod.CARD)))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(any(Order.class));
        verify(bookRepository, never()).save(any(Book.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(cartItemRepository, never()).deleteAll(any());
    }

    @Test
    void checkout_whenCartMissing_throwsEmptyCartException() {
        when(cartRepository.findByUserId(currentUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.checkout(validCheckoutRequest(PaymentMethod.CARD)))
                .isInstanceOf(EmptyCartException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkout_whenCartHasNoItems_throwsEmptyCartException() {
        when(cartRepository.findByUserId(currentUser.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.checkout(validCheckoutRequest(PaymentMethod.CARD)))
                .isInstanceOf(EmptyCartException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void checkout_whenPaymentMethodIsCod_setsPaymentStatusPending() {
        CartItem item = CartItem.builder().id(1L).cartId(cart.getId()).bookId(book.getId()).quantity(1).build();
        Order savedOrder = Order.builder().id(501L).userId(currentUser.getId()).status(OrderStatus.PLACED)
                .totalAmount(new BigDecimal("20.00")).build();

        when(cartRepository.findByUserId(currentUser.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(item));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.findByOrderId(savedOrder.getId())).thenReturn(List.of());
        when(paymentRepository.findByOrderId(savedOrder.getId())).thenReturn(
                Optional.of(Payment.builder().orderId(savedOrder.getId()).amount(new BigDecimal("20.00"))
                        .status(PaymentStatus.PENDING).paymentMethod(PaymentMethod.COD).build()));

        OrderResponse response = orderService.checkout(validCheckoutRequest(PaymentMethod.COD));

        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.COD);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(paymentCaptor.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.COD);
    }

    @Test
    void getOrderById_whenMissing_throwsOrderNotFoundException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrderById_whenOwnedByAnotherUser_throwsOrderNotFoundException() {
        Order othersOrder = Order.builder().id(5L).userId(999L).status(OrderStatus.PLACED)
                .totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findById(5L)).thenReturn(Optional.of(othersOrder));

        assertThatThrownBy(() -> orderService.getOrderById(5L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrderById_whenOwnedByCurrentUser_returnsOrder() {
        Order order = Order.builder().id(5L).userId(currentUser.getId()).status(OrderStatus.PLACED)
                .totalAmount(new BigDecimal("40.00")).build();
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(5L)).thenReturn(List.of());
        when(paymentRepository.findByOrderId(5L)).thenReturn(Optional.empty());

        OrderResponse response = orderService.getOrderById(5L);

        assertThat(response.getId()).isEqualTo(5L);
    }

    @Test
    void updateStatus_whenMissing_throwsOrderNotFoundException() {
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
        request.setStatus(OrderStatus.SHIPPED);

        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateStatus(999L, request))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void updateStatus_whenValid_updatesStatusAndSaves() {
        Order order = Order.builder().id(5L).userId(currentUser.getId()).status(OrderStatus.PLACED)
                .totalAmount(new BigDecimal("40.00")).build();
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
        request.setStatus(OrderStatus.SHIPPED);

        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(5L)).thenReturn(List.of());
        when(paymentRepository.findByOrderId(5L)).thenReturn(Optional.empty());

        OrderResponse response = orderService.updateStatus(5L, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(orderRepository).save(order);
    }
}
