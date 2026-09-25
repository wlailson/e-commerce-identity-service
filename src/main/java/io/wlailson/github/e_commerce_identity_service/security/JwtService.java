package io.wlailson.github.e_commerce_identity_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.wlailson.github.e_commerce_identity_service.domain.Role;
import io.wlailson.github.e_commerce_identity_service.domain.User;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(
                properties.secret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(User user) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
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
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
}