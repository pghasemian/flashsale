
package com.flashsale.identity.application;

import com.flashsale.exception.DuplicateResourceException;
import com.flashsale.identity.domain.Role;
import com.flashsale.identity.domain.User;
import com.flashsale.identity.dto.LoginRequest;
import com.flashsale.identity.dto.LoginResponse;
import com.flashsale.identity.dto.RegisterRequest;
import com.flashsale.identity.dto.UserResponse;
import com.flashsale.identity.persistence.RoleRepository;
import com.flashsale.identity.persistence.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "A user with this email already exists"
            );
        }

        String passwordHash = passwordEncoder.encode(request.password());

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException(
                        "Default USER role does not exist"
                ));

        User user = User.create(email, passwordHash);
        user.addRole(userRole);

        User savedUser = userRepository.save(user);

        return new UserResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getCreatedAt());
    }


    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.password(), user.getPasswordHash())) {

            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Instant now = Instant.now();
        long expiresIn = 900;

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("flashsale")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles().stream()
                        .map(Role::getName)
                        .toList())
                .build();

        JwsHeader header = JwsHeader.with(
                org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256
        ).build();

        String token = jwtEncoder.encode(
                org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                        header, claims
                )
        ).getTokenValue();

        return new LoginResponse(token, "Bearer", expiresIn);
    }

}
