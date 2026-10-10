package com.bookstore.repository.spec;

import com.bookstore.model.Book;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class BookSpecification {

    private BookSpecification() {
    }

    public static Specification<Book> search(String search) {
        return (root, query, cb) -> {
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("author")), pattern)
            );
        };
    }

    public static Specification<Book> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
    }

    public static Specification<Book> priceGreaterThanOrEqualTo(BigDecimal minPrice) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Book> priceLessThanOrEqualTo(BigDecimal maxPrice) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Book> build(String search, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        Specification<Book> spec = Specification.where(null);

        if (search != null && !search.isBlank()) {
            spec = spec.and(search(search));
        }
        if (categoryId != null) {
            spec = spec.and(hasCategoryId(categoryId));
        }
        if (minPrice != null) {
            spec = spec.and(priceGreaterThanOrEqualTo(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(priceLessThanOrEqualTo(maxPrice));
        }

        return spec;
    }
}
