package com.staynest.reviewservice.config;

import com.staynest.reviewservice.security.ReviewUserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> SUPPORTED_ROLES = Set.of("CUSTOMER", "HOST", "ADMIN");
    private final SecretKey signingKey;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret) {
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(authorization.substring(7)).getPayload();
            Object rawId = claims.get("userId");
            Object rawRoles = claims.get("roles");
            if (!(rawId instanceof Number userId) || claims.getSubject() == null || !(rawRoles instanceof List<?> roleList)) {
                unauthorized(response);
                return;
            }
            Set<String> roles = new LinkedHashSet<>();
            for (Object role : roleList) {
                if (!(role instanceof String roleName) || !SUPPORTED_ROLES.contains(roleName)) {
                    unauthorized(response);
                    return;
                }
                roles.add(roleName);
            }
            if (roles.isEmpty()) {
                unauthorized(response);
                return;
            }
            ReviewUserPrincipal principal = new ReviewUserPrincipal(userId.longValue(), claims.getSubject(), roles);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            unauthorized(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid access token");
    }
}
