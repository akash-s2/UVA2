package com.UrbanVogue.gateway.filter;

import com.UrbanVogue.gateway.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import java.util.Collections;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    System.out.println(" FILTER HIT ");
    String path = exchange.getRequest().getURI().getPath();

    // PUBLIC API  will be skip
    if (path.startsWith("/auth") ||
            path.startsWith("/user/getProducts") ||
            path.startsWith("/catalog")) {
        return chain.filter(exchange);
    }

    String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

    //  DEBUG PRINT
    System.out.println("PATH: " + path);
    System.out.println("AUTH HEADER: " + authHeader);

    //  no token
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        System.out.println(" TOKEN MISSING OR INVALID FORMAT"); // added
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    String token = authHeader.substring(7);

    try {
        //  debug
        System.out.println("TOKEN: " + token); // added

        boolean isValid = jwtUtil.validateToken(token);

        //  debug validation
        System.out.println("IS TOKEN VALID: " + isValid); // added

        if (!isValid) {
            throw new RuntimeException("Invalid Token");
        }

        //  Extract email and role
        String email = jwtUtil.extractEmail(token);
        String role = jwtUtil.extractRole(token);

        //  Debug user data
        System.out.println("EMAIL: " + email);
        System.out.println("ROLE: " + role);

        //  create authentication
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + role)
                        )
                );

        //  Setint the  security ccontext
        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    } catch (Exception e) {
        System.out.println(" TOKEN VALIDATION FAILED: " + e.getMessage());
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
    @Override
    public int getOrder() {
        return -1; // high priority
    }
}