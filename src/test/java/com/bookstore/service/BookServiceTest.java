package com.bookstore.service;

import com.bookstore.dto.BookRequest;
import com.bookstore.dto.BookResponse;
import com.bookstore.dto.PageResponse;
import com.bookstore.dto.mapper.BookMapper;
import com.bookstore.exception.BookNotFoundException;
import com.bookstore.exception.DuplicateIsbnException;
import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookService bookService;

    private BookRequest bookRequest;
    private Book book;

    @BeforeEach
    void setUp() {
        bookRequest = new BookRequest();
        bookRequest.setTitle("Clean Code");
        bookRequest.setAuthor("Robert C. Martin");
        bookRequest.setIsbn("978-0132350884");
        bookRequest.setPrice(new BigDecimal("39.99"));
        bookRequest.setStockQuantity(10);
        bookRequest.setCategoryId(1L);

        book = Book.builder()
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
    void search_buildsPageableFromPageAndSize() {
        Page<Book> emptyPage = new PageImpl<>(List.of());
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        bookService.search(null, null, null, null, 2, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(2);
        assertThat(captured.getPageSize()).isEqualTo(5);
    }

    @Test
    void search_mapsPageOfBooksToPageResponse() {
        Page<Book> bookPage = new PageImpl<>(List.of(book), PageRequest.of(0, 10), 1);
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(bookPage);

        BookResponse bookResponse = BookResponse.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("978-0132350884")
                .price(new BigDecimal("39.99"))
                .stockQuantity(10)
                .categoryId(1L)
                .build();
        when(bookMapper.toBookResponse(book)).thenReturn(bookResponse);

        PageResponse<BookResponse> result = bookService.search(null, null, null, null, 0, 10);

        assertThat(result.getContent()).containsExactly(bookResponse);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
    }

    @Test
    void create_whenIsbnExists_throwsDuplicateIsbnException() {
        when(bookRepository.existsByIsbn(bookRequest.getIsbn())).thenReturn(true);

        assertThatThrownBy(() -> bookService.create(bookRequest))
                .isInstanceOf(DuplicateIsbnException.class);

        verify(bookRepository, never()).save(any());
    }

    @Test
    void getById_whenMissing_throwsBookNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getById(99L))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void delete_whenMissing_throwsBookNotFoundException() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.delete(99L))
                .isInstanceOf(BookNotFoundException.class);

        verify(bookRepository, never()).delete(any(Book.class));
    }
}
