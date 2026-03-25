package com.UrbanVogue.user.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.UrbanVogue.user.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil; // same util (copy from gateway or shared)

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        //  skip public endpoints
        if (path.startsWith("/auth") || path.startsWith("/h2-console")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            try {
                if (jwtUtil.validateToken(token)) {

                    String email = jwtUtil.extractEmail(token);
                    String role = jwtUtil.extractRole(token);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    token,
                                    Collections.singletonList(
                                            new SimpleGrantedAuthority("ROLE_" + role)
                                    )

                            );
                    System.out.println(" USER FILTER HIT ");
                    System.out.println("ROLE: " + role);
                    System.out.println("AUTHORITIES: " + auth.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }

            } catch (Exception e) {
                // optional log
                System.out.println("JWT ERROR: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}