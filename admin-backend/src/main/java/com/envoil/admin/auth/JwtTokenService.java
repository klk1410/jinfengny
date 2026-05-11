package com.envoil.admin.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

@Service
public class JwtTokenService {

    private final JwtProperties props;

    public JwtTokenService(JwtProperties props) {
        this.props = props;
    }

    public String createToken(Long userId, String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + props.getExpireMinutes() * 60_000L);
        return Jwts.builder()
                .setSubject(username)
                .claim("uid", userId)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey signingKey() {
        byte[] raw = props.getSecret().getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            raw = Arrays.copyOf(raw, 32);
        }
        return Keys.hmacShaKeyFor(raw);
    }
}
