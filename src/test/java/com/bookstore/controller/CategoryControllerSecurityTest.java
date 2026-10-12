package com.bookstore.controller;

import com.bookstore.dto.CategoryResponse;
import com.bookstore.security.JwtAuthenticationFilter;
import com.bookstore.security.JwtService;
import com.bookstore.security.SecurityConfig;
import com.bookstore.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CategoryControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private JwtService jwtService;

    private static final String VALID_CATEGORY_JSON = """
            {
              "name": "Fiction",
              "description": "Fictional works"
            }
            """;

    private CategoryResponse sampleResponse() {
        return CategoryResponse.builder().id(1L).name("Fiction").description("Fictional works").build();
    }

    @Test
    void create_whenAnonymous_isRejected() throws Exception {
        mockMvc.perform(post("/api/categories").contentType("application/json").content(VALID_CATEGORY_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void create_whenCustomer_isForbidden() throws Exception {
        mockMvc.perform(post("/api/categories").contentType("application/json").content(VALID_CATEGORY_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_whenAdmin_isCreated() throws Exception {
        when(categoryService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/categories").contentType("application/json").content(VALID_CATEGORY_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void update_whenAnonymous_isRejected() throws Exception {
        mockMvc.perform(put("/api/categories/1").contentType("application/json").content(VALID_CATEGORY_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void update_whenCustomer_isForbidden() throws Exception {
        mockMvc.perform(put("/api/categories/1").contentType("application/json").content(VALID_CATEGORY_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_whenAdmin_isOk() throws Exception {
        when(categoryService.update(anyLong(), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/categories/1").contentType("application/json").content(VALID_CATEGORY_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void delete_whenAnonymous_isRejected() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void delete_whenCustomer_isForbidden() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_whenAdmin_isNoContent() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());
    }
}
