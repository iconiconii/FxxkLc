package com.codetop.security;

import com.codetop.entity.User;
import com.codetop.service.AuthService;
import com.codetop.util.JwtUtil;
import com.codetop.util.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT authentication filter for processing JWT tokens.
 * 
 * @author CodeTop Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final @Lazy AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtUtil.validateAccessToken(jwt)) {
                Long userId = jwtUtil.getUserIdFromAccessToken(jwt);
                
                User user = authService.getUserById(userId);
                if (user != null && user.getIsActive()) {
                    // 设置ThreadLocal用户信息
                    UserContext.setUserInfo(user.getId(), user.getUsername(), user.getEmail());
                    log.debug("JwtAuthenticationFilter: 设置用户信息到ThreadLocal - userId={}, username={}, thread={}", 
                             user.getId(), user.getUsername(), Thread.currentThread().getId());
                    
                    // 设置Spring Security上下文（保持兼容性）
                    // 确保用户具有USER角色（AuthService.getUserById已经处理了这个）
                    if (user.getRoles() == null || user.getRoles().isEmpty()) {
                        log.warn("User {} has no roles, adding default USER role", user.getUsername());
                        user.addRole("USER");
                    }
                    List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());

                    UserPrincipal userPrincipal = new UserPrincipal(user.getId(), user.getUsername(), authorities);
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userPrincipal, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("设置用户认证信息: userId={}, username={}", user.getId(), user.getUsername());
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清理ThreadLocal，防止内存泄漏
            log.debug("JwtAuthenticationFilter: 清理ThreadLocal - thread={}", Thread.currentThread().getId());
            UserContext.clear();
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}