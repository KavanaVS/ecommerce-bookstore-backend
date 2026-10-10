package com.bookstore.controller;

import com.bookstore.security.JwtAuthenticationFilter;
import com.bookstore.security.JwtService;
import com.bookstore.security.SecurityConfig;
import com.bookstore.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminOrderControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtService jwtService;

    private static final String STATUS_UPDATE_JSON = """
            { "status": "SHIPPED" }
            """;

    @Test
    void getAllOrders_whenAnonymous_isRejected() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getAllOrders_whenCustomer_isForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllOrders_whenAdmin_isOk() throws Exception {
        when(orderService.getAllOrdersForAdmin()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateStatus_whenAnonymous_isRejected() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/1/status").contentType("application/json").content(STATUS_UPDATE_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateStatus_whenCustomer_isForbidden() throws Exception {
        mockMvc.perform(patch("/api/admin/orders/1/status").contentType("application/json").content(STATUS_UPDATE_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStatus_whenAdmin_isOk() throws Exception {
        when(orderService.updateStatus(anyLong(), any())).thenReturn(null);

        mockMvc.perform(patch("/api/admin/orders/1/status").contentType("application/json").content(STATUS_UPDATE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
