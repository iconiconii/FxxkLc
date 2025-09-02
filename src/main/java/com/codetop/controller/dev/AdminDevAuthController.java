package com.codetop.controller.dev;

import com.codetop.util.JwtUtil;
import com.codetop.entity.User;
import com.codetop.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Dev-only helper to issue an ADMIN token for the current user (local testing only).
 */
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
@Tag(name = "Dev Admin Auth", description = "Dev-only admin token issuance")
public class AdminDevAuthController {

    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @PostMapping("/admin-token")
    @Operation(summary = "Issue admin token (dev only)", description = "Return a new access token including ADMIN role for current user")
    public ResponseEntity<TokenResponse> issueAdminToken(@AuthenticationPrincipal com.codetop.security.UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        User user = authService.findById(principal.getId());
        if (user == null) {
            return ResponseEntity.status(404).build();
        }
        user.addRole("USER");
        user.addRole("ADMIN");
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken, "Bearer", jwtUtil.getAccessTokenExpiration()));
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, Long expiresIn) {}
}

