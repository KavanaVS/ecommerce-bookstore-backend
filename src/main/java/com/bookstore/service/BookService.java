package com.bookstore.service;

import com.bookstore.dto.BookRequest;
import com.bookstore.dto.BookResponse;
import com.bookstore.dto.PageResponse;
import com.bookstore.dto.mapper.BookMapper;
import com.bookstore.exception.BookNotFoundException;
import com.bookstore.exception.DuplicateIsbnException;
import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.spec.BookSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
    }

    public PageResponse<BookResponse> search(String search, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                              int page, int size) {
        Specification<Book> spec = BookSpecification.build(search, categoryId, minPrice, maxPrice);
        Pageable pageable = PageRequest.of(page, size);

        Page<Book> books = bookRepository.findAll(spec, pageable);
        Page<BookResponse> responses = books.map(bookMapper::toBookResponse);

        return PageResponse.of(responses);
    }

    public BookResponse getById(Long id) {
        Book book = findBookOrThrow(id);
        return bookMapper.toBookResponse(book);
    }

    public BookResponse create(BookRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new DuplicateIsbnException("A book with this ISBN already exists");
        }

        Book book = bookMapper.toBook(request);
        Book saved = bookRepository.save(book);
        return bookMapper.toBookResponse(saved);
    }

    public BookResponse update(Long id, BookRequest request) {
        Book book = findBookOrThrow(id);

        boolean isbnChanged = !book.getIsbn().equals(request.getIsbn());
        if (isbnChanged && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new DuplicateIsbnException("A book with this ISBN already exists");
        }

        bookMapper.updateBookFromRequest(request, book);
        Book saved = bookRepository.save(book);
        return bookMapper.toBookResponse(saved);
    }

    public void delete(Long id) {
        Book book = findBookOrThrow(id);
        bookRepository.delete(book);
    }

    private Book findBookOrThrow(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + id));
    }
}
