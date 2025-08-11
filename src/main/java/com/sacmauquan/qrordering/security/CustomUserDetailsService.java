// src/main/java/com/sacmauquan/qrordering/security/CustomUserDetailsService.java
package com.sacmauquan.qrordering.security;

import com.sacmauquan.qrordering.model.User;
import com.sacmauquan.qrordering.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User u = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    // role "MANAGER"/"STAFF" -> authority "ROLE_MANAGER"/"ROLE_STAFF"
    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()));
    return new org.springframework.security.core.userdetails.User(
        u.getEmail(), u.getPassword(), authorities
    );
  }
}
