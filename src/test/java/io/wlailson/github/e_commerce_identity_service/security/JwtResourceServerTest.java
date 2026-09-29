package io.wlailson.github.e_commerce_identity_service.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtResourceServerTest {

    @Test
    void decodesIdentityTokenAndMapsItsRolesToAuthorities() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        JwtProperties properties = new JwtProperties(
                pem("PRIVATE KEY", keyPair.getPrivate().getEncoded()),
                pem("PUBLIC KEY", keyPair.getPublic().getEncoded()),
                300
        );
        JwtConfig jwtConfig = new JwtConfig();
        RSAPublicKey publicKey = jwtConfig.publicKey(properties);
        JwtDecoder decoder = jwtConfig.jwtDecoder(publicKey);

        String token = Jwts.builder()
                .subject("user@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .claim("roles", List.of("ROLE_USER"))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        Jwt jwt = decoder.decode(token);
        Authentication authentication =
                new SecurityConfig().jwtAuthenticationConverter().convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("user@example.com");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_USER");
    }

    private static String pem(String type, byte[] encodedKey) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encodedKey)
                + "\n-----END " + type + "-----";
    }
}
