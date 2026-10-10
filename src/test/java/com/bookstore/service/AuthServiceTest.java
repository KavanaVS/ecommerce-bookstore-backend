package com.bookstore.service;

import com.bookstore.dto.AuthResponse;
import com.bookstore.dto.LoginRequest;
import com.bookstore.dto.RegisterRequest;
import com.bookstore.dto.UserResponse;
import com.bookstore.dto.mapper.UserMapper;
import com.bookstore.exception.DuplicateEmailException;
import com.bookstore.exception.InvalidCredentialsException;
import com.bookstore.model.Role;
import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Jane Doe");
        registerRequest.setEmail("jane@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("jane@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void register_whenEmailAlreadyExists_throwsDuplicateEmailException() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenEmailIsNew_savesUserAndReturnsResponse() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashed-password");

        User savedUser = User.builder()
                .id(1L)
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .passwordHash("hashed-password")
                .role(Role.CUSTOMER)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponse expectedResponse = UserResponse.builder()
                .id(1L)
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .role(Role.CUSTOMER)
                .build();
        when(userMapper.toUserResponse(savedUser)).thenReturn(expectedResponse);

        UserResponse result = authService.register(registerRequest);

        assertThat(result).isEqualTo(expectedResponse);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User persisted = userCaptor.getValue();
        assertThat(persisted.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(persisted.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(persisted.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void login_whenEmailNotFound_throwsInvalidCredentialsException() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_whenPasswordIncorrect_throwsInvalidCredentialsException() {
        User user = User.builder()
                .id(1L)
                .name("Jane Doe")
                .email(loginRequest.getEmail())
                .passwordHash("hashed-password")
                .role(Role.CUSTOMER)
                .build();
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_whenCredentialsValid_returnsAuthResponseWithToken() {
        User user = User.builder()
                .id(1L)
                .name("Jane Doe")
                .email(loginRequest.getEmail())
                .passwordHash("hashed-password")
                .role(Role.CUSTOMER)
                .build();
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(user.getId(), user.getEmail(), user.getRole())).thenReturn("signed-jwt");

        AuthResponse result = authService.login(loginRequest);

        assertThat(result.getToken()).isEqualTo("signed-jwt");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo(loginRequest.getEmail());
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
    }
}
