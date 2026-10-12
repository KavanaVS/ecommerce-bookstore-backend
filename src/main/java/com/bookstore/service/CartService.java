package com.bookstore.service;

import com.bookstore.dto.CartItemRequest;
import com.bookstore.dto.CartItemResponse;
import com.bookstore.dto.CartItemUpdateRequest;
import com.bookstore.dto.CartResponse;
import com.bookstore.exception.BookNotFoundException;
import com.bookstore.exception.CartItemNotFoundException;
import com.bookstore.exception.InsufficientStockException;
import com.bookstore.exception.InvalidCredentialsException;
import com.bookstore.model.Book;
import com.bookstore.model.Cart;
import com.bookstore.model.CartItem;
import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartItemRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                        BookRepository bookRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    public CartResponse getCart() {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId()).orElse(null);

        if (cart == null) {
            return CartResponse.builder()
                    .id(null)
                    .items(List.of())
                    .total(BigDecimal.ZERO)
                    .build();
        }

        return toCartResponse(cart);
    }

    public CartResponse addItem(CartItemRequest request) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(user.getId()).build()));

        Book book = findBookOrThrow(request.getBookId());
        CartItem item = cartItemRepository.findByCartIdAndBookId(cart.getId(), request.getBookId()).orElse(null);

        int newQuantity = (item == null ? 0 : item.getQuantity()) + request.getQuantity();
        validateStock(book, newQuantity);

        if (item == null) {
            item = CartItem.builder()
                    .cartId(cart.getId())
                    .bookId(request.getBookId())
                    .quantity(newQuantity)
                    .build();
        } else {
            item.setQuantity(newQuantity);
        }
        cartItemRepository.save(item);

        return toCartResponse(cart);
    }

    public CartResponse updateItemQuantity(Long itemId, CartItemUpdateRequest request) {
        CartItem item = findOwnedCartItemOrThrow(itemId);
        Book book = findBookOrThrow(item.getBookId());

        validateStock(book, request.getQuantity());

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        Cart cart = cartRepository.findById(item.getCartId())
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found with id: " + itemId));
        return toCartResponse(cart);
    }

    public void removeItem(Long itemId) {
        CartItem item = findOwnedCartItemOrThrow(itemId);
        cartItemRepository.delete(item);
    }

    private CartItem findOwnedCartItemOrThrow(Long itemId) {
        User user = getCurrentUser();
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found with id: " + itemId));

        Cart cart = cartRepository.findById(item.getCartId())
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found with id: " + itemId));

        if (!cart.getUserId().equals(user.getId())) {
            throw new CartItemNotFoundException("Cart item not found with id: " + itemId);
        }

        return item;
    }

    private void validateStock(Book book, int requestedQuantity) {
        if (requestedQuantity > book.getStockQuantity()) {
            throw new InsufficientStockException("Insufficient stock for book: " + book.getTitle());
        }
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

    private CartResponse toCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toCartItemResponse)
                .toList();

        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .total(total)
                .build();
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        Book book = findBookOrThrow(item.getBookId());
        BigDecimal lineTotal = book.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponse.builder()
                .id(item.getId())
                .bookId(book.getId())
                .title(book.getTitle())
                .quantity(item.getQuantity())
                .unitPrice(book.getPrice())
                .lineTotal(lineTotal)
                .build();
    }
}
