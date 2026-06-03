package com.sacmauquan.qrordering.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
public class JwtService {

  private final Key key;
  private final long expirationMs;
  private final long refreshExpirationMs;

  /**
   * Initializes the JwtService with secret key and expiration time from configuration.
   * 
   * @param secret The secret key used for signing tokens
   * @param expirationMs The duration (in milliseconds) before a token expires
   */
  public JwtService(@Value("${security.jwt.secret}") String secret,
                    @Value("${security.jwt.expiration-ms}") long expirationMs,
                    @Value("${security.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
    this.refreshExpirationMs = refreshExpirationMs;
  }

  /**
   * Validates the JWT secret key length at startup.
   * HMAC-SHA256 requires a 256-bit (32-byte) key. A shorter key silently causes
   * weaker security. This check logs a warning but won't crash the application
   * for backward compatibility.
   */
  @PostConstruct
  public void validateSecretStrength() {
    byte[] keyBytes = key.getEncoded();
    if (keyBytes.length < 32) {
      log.warn("JWT secret is only {} bytes — HMAC-SHA256 requires at least 32 bytes (256 bits) for full strength. "
              + "Consider using a longer secret via the JWT_SECRET environment variable.",
              keyBytes.length);
    }
  }

  /**
   * Generates a new JWT for a specific subject with optional custom claims.
   * 
   * @param subject The subject of the token (e.g., user email)
   * @param claims Map of additional data to include in the token payload
   * @return The signed JWT string
   */
  public String generateToken(String subject, Map<String, Object> claims) {
    return generateToken(subject, claims, expirationMs);
  }

  public String generateAccessToken(String subject, Map<String, Object> claims) {
    return generateToken(subject, withType(claims, "access"), expirationMs);
  }

  public String generateRefreshToken(String subject, Map<String, Object> claims) {
    return generateToken(subject, withType(claims, "refresh"), refreshExpirationMs);
  }

  private String generateToken(String subject, Map<String, Object> claims, long ttlMs) {
    Date now = new Date();
    Date exp = new Date(now.getTime() + ttlMs);
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setIssuedAt(now)
        .setExpiration(exp)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  private Map<String, Object> withType(Map<String, Object> claims, String tokenType) {
    Map<String, Object> typedClaims = new java.util.HashMap<>(claims);
    typedClaims.put("typ", tokenType);
    return typedClaims;
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

  public String extractTokenType(String token) {
    Object type = parse(token).getBody().get("typ");
    return type != null ? type.toString() : null;
  }

  public Object extractClaim(String token, String claimName) {
    return parse(token).getBody().get(claimName);
  }

  public boolean isRefreshToken(String token) {
    return isValid(token) && "refresh".equals(extractTokenType(token));
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
