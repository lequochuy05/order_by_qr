package com.qros.modules.auth.service;

import com.qros.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

/**
 * CustomUserDetailsService - Implementation of Spring Security's
 * UserDetailsService.
 * Responsible for retrieving user authentication data from the database using
 * email.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
  private final UserRepository userRepository;

  /**
   * Loads user details based on the provided email address.
   * 
   * @param email The user's email address
   * @return UserDetails implementation (the User entity)
   * @throws UsernameNotFoundException if no user is found with the given email
   */
  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return userRepository.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
  }
}
