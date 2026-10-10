package com.bookstore.controller;

import com.bookstore.dto.BookResponse;
import com.bookstore.security.JwtAuthenticationFilter;
import com.bookstore.security.JwtService;
import com.bookstore.security.SecurityConfig;
import com.bookstore.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class BookControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @MockBean
    private JwtService jwtService;

    private static final String VALID_BOOK_JSON = """
            {
              "title": "Clean Code",
              "author": "Robert C. Martin",
              "isbn": "978-0132350884",
              "price": 39.99,
              "stockQuantity": 10,
              "categoryId": 1
            }
            """;

    private BookResponse sampleResponse() {
        return BookResponse.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("978-0132350884")
                .price(new BigDecimal("39.99"))
                .stockQuantity(10)
                .categoryId(1L)
                .build();
    }

    @Test
    void create_whenAnonymous_isRejected() throws Exception {
        mockMvc.perform(post("/api/books").contentType("application/json").content(VALID_BOOK_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void create_whenCustomer_isForbidden() throws Exception {
        mockMvc.perform(post("/api/books").contentType("application/json").content(VALID_BOOK_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_whenAdmin_isCreated() throws Exception {
        when(bookService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/books").contentType("application/json").content(VALID_BOOK_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void update_whenAnonymous_isRejected() throws Exception {
        mockMvc.perform(put("/api/books/1").contentType("application/json").content(VALID_BOOK_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void update_whenCustomer_isForbidden() throws Exception {
        mockMvc.perform(put("/api/books/1").contentType("application/json").content(VALID_BOOK_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_whenAdmin_isOk() throws Exception {
        when(bookService.update(anyLong(), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/books/1").contentType("application/json").content(VALID_BOOK_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void delete_whenAnonymous_isRejected() throws Exception {
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void delete_whenCustomer_isForbidden() throws Exception {
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_whenAdmin_isNoContent() throws Exception {
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isNoContent());
    }
}
