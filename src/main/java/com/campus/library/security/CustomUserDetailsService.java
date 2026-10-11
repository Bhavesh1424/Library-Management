package com.campus.library.security;

import com.campus.library.domain.User;
import com.campus.library.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Allows login via email or university ID
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUniversityId(identifier))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email or university ID: " + identifier));

        return new CustomUserDetails(user);
    }
}
