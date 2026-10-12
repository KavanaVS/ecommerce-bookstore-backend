package com.bookstore.service;

import com.bookstore.dto.CartItemRequest;
import com.bookstore.dto.CartItemUpdateRequest;
import com.bookstore.dto.CartResponse;
import com.bookstore.exception.CartItemNotFoundException;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.model.Book;
import com.bookstore.model.Cart;
import com.bookstore.model.CartItem;
import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartItemRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User currentUser;
    private Book book;
    private Cart cart;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).email("customer1@example.com").build();
        book = Book.builder().id(10L).title("Clean Code").price(new BigDecimal("39.99")).stockQuantity(5).build();
        cart = Cart.builder().id(100L).userId(currentUser.getId()).build();

        var authentication = new UsernamePasswordAuthenticationToken(currentUser.getEmail(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findByEmail(currentUser.getEmail())).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addItem_whenNewBook_createsCartAndItem() {
        CartItemRequest request = new CartItemRequest();
        request.setBookId(book.getId());
        request.setQuantity(2);

        when(cartRepository.findByUserId(currentUser.getId())).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(cartItemRepository.findByCartIdAndBookId(cart.getId(), book.getId())).thenReturn(Optional.empty());
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(
                List.of(CartItem.builder().id(1L).cartId(cart.getId()).bookId(book.getId()).quantity(2).build()));

        CartResponse response = cartService.addItem(request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getTotal()).isEqualByComparingTo("79.98");
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addItem_whenBookAlreadyInCart_incrementsQuantity() {
        CartItemRequest request = new CartItemRequest();
        request.setBookId(book.getId());
        request.setQuantity(2);

        CartItem existingItem = CartItem.builder().id(1L).cartId(cart.getId()).bookId(book.getId()).quantity(1).build();

        when(cartRepository.findByUserId(currentUser.getId())).thenReturn(Optional.of(cart));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(cartItemRepository.findByCartIdAndBookId(cart.getId(), book.getId())).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(existingItem));

        cartService.addItem(request);

        assertThat(existingItem.getQuantity()).isEqualTo(3);
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    void addItem_whenRequestedQuantityExceedsStock_throwsInsufficientStockException() {
        CartItemRequest request = new CartItemRequest();
        request.setBookId(book.getId());
        request.setQuantity(10);

        when(cartRepository.findByUserId(currentUser.getId())).thenReturn(Optional.of(cart));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(cartItemRepository.findByCartIdAndBookId(cart.getId(), book.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(request))
                .isInstanceOf(InsufficientStockException.class);

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void updateItemQuantity_whenValid_updatesQuantity() {
        CartItem item = CartItem.builder().id(5L).cartId(cart.getId()).bookId(book.getId()).quantity(1).build();
        CartItemUpdateRequest request = new CartItemUpdateRequest();
        request.setQuantity(4);

        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(item));

        cartService.updateItemQuantity(5L, request);

        assertThat(item.getQuantity()).isEqualTo(4);
        verify(cartItemRepository).save(item);
    }

    @Test
    void updateItemQuantity_whenExceedsStock_throwsInsufficientStockException() {
        CartItem item = CartItem.builder().id(5L).cartId(cart.getId()).bookId(book.getId()).quantity(1).build();
        CartItemUpdateRequest request = new CartItemUpdateRequest();
        request.setQuantity(99);

        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> cartService.updateItemQuantity(5L, request))
                .isInstanceOf(InsufficientStockException.class);

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void updateItemQuantity_whenItemBelongsToAnotherUser_throwsCartItemNotFoundException() {
        Cart otherUsersCart = Cart.builder().id(200L).userId(999L).build();
        CartItem item = CartItem.builder().id(5L).cartId(otherUsersCart.getId()).bookId(book.getId()).quantity(1).build();
        CartItemUpdateRequest request = new CartItemUpdateRequest();
        request.setQuantity(2);

        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(cartRepository.findById(otherUsersCart.getId())).thenReturn(Optional.of(otherUsersCart));

        assertThatThrownBy(() -> cartService.updateItemQuantity(5L, request))
                .isInstanceOf(CartItemNotFoundException.class);

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void removeItem_whenMissing_throwsCartItemNotFoundException() {
        when(cartItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(99L))
                .isInstanceOf(CartItemNotFoundException.class);

        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }

    @Test
    void removeItem_whenOwnedByCurrentUser_deletesItem() {
        CartItem item = CartItem.builder().id(5L).cartId(cart.getId()).bookId(book.getId()).quantity(1).build();

        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(cartRepository.findById(cart.getId())).thenReturn(Optional.of(cart));

        cartService.removeItem(5L);

        verify(cartItemRepository).delete(item);
    }

    @Test
    void getCart_whenNoCartExists_returnsEmptyCartWithZeroTotal() {
        when(cartRepository.findByUserId(currentUser.getId())).thenReturn(Optional.empty());

        CartResponse response = cartService.getCart();

        assertThat(response.getId()).isNull();
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getCart_whenItemsExist_computesTotalCorrectly() {
        CartItem item1 = CartItem.builder().id(1L).cartId(cart.getId()).bookId(book.getId()).quantity(2).build();
        Book secondBook = Book.builder().id(20L).title("Effective Java").price(new BigDecimal("10.00")).stockQuantity(5).build();
        CartItem item2 = CartItem.builder().id(2L).cartId(cart.getId()).bookId(secondBook.getId()).quantity(3).build();

        when(cartRepository.findByUserId(currentUser.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(item1, item2));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));
        when(bookRepository.findById(secondBook.getId())).thenReturn(Optional.of(secondBook));

        CartResponse response = cartService.getCart();

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getTotal()).isEqualByComparingTo("109.98");
    }
}
