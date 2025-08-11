// src/main/java/com/sacmauquan/qrordering/security/JwtService.java
package com.sacmauquan.qrordering.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

  private final Key key;
  private final long expirationMs;

  @Autowired
  public JwtService(@Value("${security.jwt.secret}") String secret,
                    @Value("${security.jwt.expiration-ms}") long expirationMs) {
    // secret phải đủ dài, tốt nhất ≥ 32 bytes
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  public String generateToken(String subject, Map<String, Object> claims) {
    Date now = new Date();
    Date exp = new Date(now.getTime() + expirationMs);
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setIssuedAt(now)
        .setExpiration(exp)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public boolean isValid(String token) {
    try { parse(token); return true; }
    catch (JwtException | IllegalArgumentException e) { return false; }
  }

  public String extractSubject(String token) {
    return parse(token).getBody().getSubject();
  }

  private Jws<Claims> parse(String token) {
    return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
  }
}
