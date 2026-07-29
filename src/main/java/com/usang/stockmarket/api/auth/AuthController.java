package com.usang.stockmarket.api.auth;

import com.usang.stockmarket.api.dto.ApiResponse;
import com.usang.stockmarket.application.auth.AuthService;
import com.usang.stockmarket.infra.security.JwtAuthenticationResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtAuthenticationResolver jwtAuthenticationResolver;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(@RequestBody LoginParamDto loginParamDto) {
        String token = authService.login(loginParamDto.email(), loginParamDto.password());
        ResponseCookie cookie = jwtAuthenticationResolver.buildAuthCookie(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success("로그인 되었습니다."));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        ResponseCookie cookie = jwtAuthenticationResolver.buildLogoutCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success("로그아웃 되었습니다."));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Void>> me() {
        return ResponseEntity.ok(ApiResponse.success("인증된 사용자입니다."));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody SignupParamDto signupParamDto) {
        authService.signup(signupParamDto.email(), signupParamDto.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("회원가입이 완료되었습니다."));
    }

}

record LoginParamDto (String email, String password) {
    public LoginParamDto {
        if(!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("이메일을 입력해 주세요.");
        }
        if(!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }
    }
}

record SignupParamDto (String email, String password) {
    public SignupParamDto {
        if(!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("이메일을 입력해 주세요.");
        }
        if(!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }
    }
}