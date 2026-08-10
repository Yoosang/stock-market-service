package com.usang.stockmarket.application.auth;

import com.usang.stockmarket.domain.user.User;
import com.usang.stockmarket.domain.user.UserRepository;
import com.usang.stockmarket.infra.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void 이미_사용중인_이메일이면_회원가입시_예외를_던진다() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mock(User.class)));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.signup("test@test.com", "password"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void 회원가입_성공시_비밀번호를_인코딩하여_저장한다() {
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");

        authService.signup("new@test.com", "rawPassword");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("new@test.com", captor.getValue().getEmail());
        assertEquals("encodedPassword", captor.getValue().getPasswordHash());
    }

    @Test
    void 존재하지않는_이메일로_로그인하면_예외를_던진다() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login("unknown@test.com", "password"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void 비밀번호가_틀리면_로그인시_예외를_던진다() {
        User user = new User("test@test.com", "encodedPassword");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login("test@test.com", "wrongPassword"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void 로그인_성공시_JWT_토큰을_발급한다() {
        User user = new User("test@test.com", "encodedPassword");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(user.getId())).thenReturn("issued-jwt-token");

        String token = authService.login("test@test.com", "rawPassword");

        assertEquals("issued-jwt-token", token);
    }
}
