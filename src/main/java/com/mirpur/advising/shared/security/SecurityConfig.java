package com.mirpur.advising.shared.security;

import com.mirpur.advising.shared.config.RuntimeSettings;
import com.mirpur.advising.shared.db.JdbcDatabase;
import com.mirpur.advising.account.SecurityRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean SecretKey jwtKey(RuntimeSettings settings) {
        return Keys.hmacShaKeyFor(settings.app().jwtSecret().getBytes(StandardCharsets.UTF_8));
    }
    @Bean SecurityFilterChain security(HttpSecurity http, SecurityRepository db, SecretKey key) throws Exception {
        var filter = new OncePerRequestFilter() {
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws java.io.IOException, ServletException {
                String auth = req.getHeader("Authorization");
                if (auth != null && auth.startsWith("Bearer ")) {
                    try {
                        var c = Jwts.parser().verifyWith(key).build().parseSignedClaims(auth.substring(7)).getPayload();
                        var u = db.queryUsersRecord(Long.parseLong(c.getSubject()));
                        if (Boolean.TRUE.equals(u.get("active")) || "1".equals(String.valueOf(u.get("active")))) SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(JdbcDatabase.number(u, "id"), null, List.of(new SimpleGrantedAuthority("ROLE_" + u.get("role")))));
                    } catch (Exception ignored) {
                        SecurityContextHolder.clearContext();
                    }
                }
                chain.doFilter(req, res);
            }
        };
        return http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a -> a.requestMatchers("/api/auth/login", "/api/auth/setup-password", "/actuator/health").permitAll().requestMatchers("/api/admin/**").hasRole("ADMIN").requestMatchers("/api/teacher/**").hasRole("TEACHER").requestMatchers("/api/advising/**", "/api/routine/**", "/api/drop/**").hasRole("STUDENT").requestMatchers("/api/**").authenticated().anyRequest().permitAll()).exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"message\":\"Please sign in.\"}");
        }).accessDeniedHandler((req, res, ex) -> {
            res.setStatus(403);
            res.setContentType("application/json");
            res.getWriter().write("{\"message\":\"Access denied.\"}");
        })).headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; form-action 'self'"))).addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class).build();
    }
}
