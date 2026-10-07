package io.wlailson.github.e_commerce_identity_service.security;

import io.jsonwebtoken.Jwts;
import io.wlailson.github.e_commerce_identity_service.domain.Role;
import io.wlailson.github.e_commerce_identity_service.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;


@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;
    private final PrivateKey privateKey;

    public String generateToken(User user) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .claim("userId", user.getId())
                .claim(
                        "roles",
                        user.getRoles()
                                .stream()
                                .map(Role::getAuthority)
                                .toArray(String[]::new)
                )
                .expiration(
                        Date.from(
                                now.plusSeconds(properties.duration())
                        )
                )
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }
}
