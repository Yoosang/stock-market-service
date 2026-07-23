package com.usang.stockmarket.api.auth;

import com.usang.stockmarket.api.dto.ApiResponse;
import com.usang.stockmarket.application.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public String login(@RequestBody LoginParamDto loginParamDto) {
        return authService.login(loginParamDto.email(), loginParamDto.password());
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