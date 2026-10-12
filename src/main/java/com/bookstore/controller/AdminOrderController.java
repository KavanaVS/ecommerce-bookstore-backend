package com.bookstore.controller;

import com.bookstore.dto.ApiResponse;
import com.bookstore.dto.OrderResponse;
import com.bookstore.dto.OrderStatusUpdateRequest;
import com.bookstore.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders() {
        List<OrderResponse> response = orderService.getAllOrdersForAdmin();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(@PathVariable Long id,
                                                                      @Valid @RequestBody OrderStatusUpdateRequest request) {
        OrderResponse response = orderService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
