package com.sacmauquan.qrordering.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * JwtService - Service for handling JSON Web Tokens (JWT).
 * Responsible for token generation, validation, and claim extraction.
 */
@Service
public class JwtService {

  private final Key key;
  private final long expirationMs;

  /**
   * Initializes the JwtService with secret key and expiration time from configuration.
   * 
   * @param secret The secret key used for signing tokens
   * @param expirationMs The duration (in milliseconds) before a token expires
   */
  public JwtService(@Value("${security.jwt.secret}") String secret,
                    @Value("${security.jwt.expiration-ms}") long expirationMs) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  /**
   * Generates a new JWT for a specific subject with optional custom claims.
   * 
   * @param subject The subject of the token (e.g., user email)
   * @param claims Map of additional data to include in the token payload
   * @return The signed JWT string
   */
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

  /**
   * Validates the integrity and expiration of a JWT string.
   * 
   * @param token The JWT string to validate
   * @return true if the token is valid, false otherwise
   */
  public boolean isValid(String token) {
    try { 
        parse(token); 
        return true; 
    } catch (JwtException | IllegalArgumentException e) { 
        return false; 
    }
  }

  /**
   * Extracts the subject (e.g., email) from a valid JWT.
   * 
   * @param token The JWT string
   * @return The subject string
   */
  public String extractSubject(String token) {
    return parse(token).getBody().getSubject();
  }

  /**
   * Parses the JWT and retrieves its claims using the signing key.
   * 
   * @param token The JWT string
   * @return Jws object containing the claims
   */
  private Jws<Claims> parse(String token) {
    return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
  }
}
