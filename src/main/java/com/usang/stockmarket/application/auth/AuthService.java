package com.usang.stockmarket.application.auth;

import com.usang.stockmarket.domain.user.User;
import com.usang.stockmarket.domain.user.UserRepository;
import com.usang.stockmarket.infra.security.JwtTokenProvider;
import com.usang.stockmarket.infra.security.LoginRateLimitConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginRateLimitConfiguration loginRateLimitConfiguration;

    public void signup(String email, String password) {
        if(userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        User user = new User(email, passwordEncoder.encode(password));
        userRepository.save(user);
    }

    @Transactional
    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 틀렸습니다."));

        if (user.isLocked()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "비밀번호 오류 횟수를 초과하여 계정이 잠겼습니다. 잠시 후 다시 시도해 주세요.");
        }

        if(!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.incrementFailedLoginCount();
            int remainingAttempts = loginRateLimitConfiguration.maxAttemptsPerEmail() - user.getFailedLoginCount();
            if (remainingAttempts <= 0) {
                user.lockUntil(LocalDateTime.now().plusSeconds(loginRateLimitConfiguration.windowSeconds()));
            }
            throw new LoginFailedException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 틀렸습니다.", Math.max(remainingAttempts, 0));
        }

        user.resetLoginFailure();
        return jwtTokenProvider.generateToken(user.getId());
    }

}