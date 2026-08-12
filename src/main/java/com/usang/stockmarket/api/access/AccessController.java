package com.usang.stockmarket.api.access;

import com.usang.stockmarket.api.dto.ApiResponse;
import com.usang.stockmarket.application.access.VisitTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/access")
public class AccessController {
    private final VisitTrackingService visitTrackingService;

    @PostMapping("/visit")
    public ResponseEntity<ApiResponse<Void>> visit(HttpServletRequest request) {
        visitTrackingService.trackMainPageVisit(request.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("기록되었습니다."));
    }
}
