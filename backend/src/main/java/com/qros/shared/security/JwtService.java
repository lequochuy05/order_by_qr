package com.qros.shared.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;

/**
 * JwtService - Service for handling JSON Web Tokens (JWT).
 * Responsible for token generation, validation, and claim extraction.
 */
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

  public String generateAccessToken(String subject, Map<String, Object> claims) {
    return generateToken(subject, claims, expirationMs, "access");
  }

  public String generateRefreshToken(String subject, Map<String, Object> claims) {
    return generateToken(subject, claims, refreshExpirationMs, "refresh");
  }

  private String generateToken(String subject, Map<String, Object> claims, long ttlMs, String tokenType) {
    Date now = new Date();
    Date exp = new Date(now.getTime() + ttlMs);

    Map<String, Object> typedClaims = new HashMap<>(claims);
    typedClaims.put("typ", tokenType);
    Object jti = typedClaims.remove("jti");

    JwtBuilder builder = Jwts.builder()
        .setClaims(typedClaims)
        .setSubject(subject)
        .setIssuedAt(now)
        .setExpiration(exp)
        .signWith(key, SignatureAlgorithm.HS256);

    if (jti != null) {
      builder.setId(jti.toString());
    }

    return builder.compact();
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

  public boolean isAccessToken(String token) {
    return hasTokenType(token, "access");
  }

  public boolean isRefreshToken(String token) {
      return hasTokenType(token, "refresh");
  }

  private boolean hasTokenType(String token, String expectedType) {
      try {
          Claims claims = parse(token).getBody();
          return expectedType.equals(claims.get("typ", String.class));
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
    return parse(token).getBody().get("typ", String.class);
  }

  public String extractJti(String token) {
    return parse(token).getBody().getId();
  }

  public Object extractClaim(String token, String claimName) {
    return parse(token).getBody().get(claimName);
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
