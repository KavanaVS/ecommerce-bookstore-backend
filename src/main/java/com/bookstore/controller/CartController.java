package com.bookstore.controller;

import com.bookstore.dto.ApiResponse;
import com.bookstore.dto.CartItemRequest;
import com.bookstore.dto.CartItemUpdateRequest;
import com.bookstore.dto.CartResponse;
import com.bookstore.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        CartResponse response = cartService.getCart();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(@Valid @RequestBody CartItemRequest request) {
        CartResponse response = cartService.addItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(@PathVariable Long id,
                                                                         @Valid @RequestBody CartItemUpdateRequest request) {
        CartResponse response = cartService.updateItemQuantity(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> removeItem(@PathVariable Long id) {
        cartService.removeItem(id);
        return ResponseEntity.noContent().build();
    }
}
