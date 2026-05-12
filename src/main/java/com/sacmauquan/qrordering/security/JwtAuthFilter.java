package com.sacmauquan.qrordering.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;

/**
 * JwtAuthFilter - Security filter that intercepts each HTTP request to validate JWT tokens.
 * Extracts the token from the 'Authorization' header and establishes security context.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  /**
   * Performs the filtering logic: validates the Bearer token and sets authentication in SecurityContext.
   * 
   * @param request The incoming HTTP request
   * @param response The outgoing HTTP response
   * @param chain The filter chain
   * @throws ServletException if a servlet error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {

    final String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      chain.doFilter(request, response);
      return;
    }

    final String token = authHeader.substring(7);
    if (jwtService.isValid(token)) {
      String email = jwtService.extractSubject(token);
      UserDetails userDetails = userDetailsService.loadUserByUsername(email);

      var auth = new UsernamePasswordAuthenticationToken(
          userDetails, null, userDetails.getAuthorities());
      auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(auth);
    }
    chain.doFilter(request, response);
  }
}
