package com.bookstore.dto.mapper;

import com.bookstore.dto.BookRequest;
import com.bookstore.dto.BookResponse;
import com.bookstore.model.Book;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BookMapper {

    BookResponse toBookResponse(Book book);

    Book toBook(BookRequest request);

    void updateBookFromRequest(BookRequest request, @MappingTarget Book book);
}
