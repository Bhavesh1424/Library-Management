package com.campus.library.service;

import com.campus.library.domain.User;
import com.campus.library.domain.enums.UserRole;
import com.campus.library.dto.auth.LoginRequest;
import com.campus.library.dto.auth.LoginResponse;
import com.campus.library.dto.auth.RegisterRequest;
import com.campus.library.exception.BadRequestException;
import com.campus.library.repository.UserRepository;
import com.campus.library.security.CustomUserDetails;
import com.campus.library.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final long jwtExpirationMs;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       @Value("${app.jwt.expiration-ms:86400000}") long jwtExpirationMs) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new BadRequestException("User record not found"));

        return new LoginResponse(
                jwt,
                "Bearer",
                user.getId(),
                user.getUniversityId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                jwtExpirationMs
        );
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already registered: " + request.email());
        }
        if (userRepository.existsByUniversityId(request.universityId())) {
            throw new BadRequestException("University ID already registered: " + request.universityId());
        }

        User user = new User(
                request.universityId(),
                request.fullName(),
                request.email(),
                passwordEncoder.encode(request.password()),
                UserRole.ROLE_STUDENT
        );

        User savedUser = userRepository.save(user);

        return login(new LoginRequest(savedUser.getEmail(), request.password()));
    }

    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new org.springframework.security.authentication.BadCredentialsException("No authenticated user in current security context");
        }
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new BadRequestException("User profile not found"));
    }
}
