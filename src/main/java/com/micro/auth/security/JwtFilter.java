package com.micro.auth.security;

import com.micro.auth.entity.User;
import com.micro.auth.exception.ApiException;
import com.micro.auth.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/register",
            "/api/auth/verify-email",
            "/api/auth/resent-verification",
            "/api/auth/login",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/refresh-token");

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenBlackListService tokenBlackListService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getServletPath();
        String authHeader = request.getHeader("Authorization");
        boolean isPublic = PUBLIC_ENDPOINTS.contains(path);

        if (isPublic)
            filterChain.doFilter(request, response);
        else {
            if (null == authHeader || !authHeader.startsWith("Bearer ")) {
//                if (path.contains("swagger") || path.contains("v3-docs")) {
                filterChain.doFilter(request, response);
                return;
//                }
//                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                response.getWriter().write("Authorization header missing");
//                return;
            }

            String accessToken = authHeader.substring(7);
            if (tokenBlackListService.isBlacklisted(accessToken)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("""
                        {
                            "apiStatus": false,
                            "message": "Invalid or expired token"
                        }
                        """);
                return;
            }

            try {
                String email = jwtService.extractEmail(accessToken);
                User user = userRepository.findByEmail(email).orElseThrow(() -> ApiException.notFound("User not found"));
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, List.of(authority));
                authentication.setDetails(accessToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                filterChain.doFilter(request, response);
            } catch (Exception ex) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("""
                        {
                            "apiStatus": false,
                            "message": "%s"
                        }
                        """.formatted(ex.getMessage())
                );
            }
        }
    }
}
