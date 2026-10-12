package com.bookstore.dto.mapper;

import com.bookstore.dto.UserResponse;
import com.bookstore.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toUserResponse(User user);
}
