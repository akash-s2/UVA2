//
//// here we are actually genrating the jwt tokens
//
//package com.UrbanVogue.user.AuthModule.jwt;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
//import io.jsonwebtoken.security.Keys;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import javax.crypto.SecretKey;
//import java.nio.charset.StandardCharsets;
//import java.util.Date;
//
//@Component
//public class JwtUtil {
//
//    @Value("${jwt.secret}")
//    private String SECRET;
//
//    @Value("${jwt.expiration}")
//    private long EXPIRATION;
//
//    public String generateToken(String email,String role) {
//
//        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
//
//        return Jwts.builder()
//                .setSubject(email)
//                .claim("role", role)
//                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
//                .signWith(key, SignatureAlgorithm.HS256)
//                .compact();
//    }
//}