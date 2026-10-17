package com.example.camel.payment.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // Chave secreta segura (em produção, viria de variáveis de ambiente/Vault)
    private static final String SECRET = "minha-chave-secreta-super-segura-para-assinatura-jwt-123456";
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public String gerarToken(String clientId) {
        long agoraMillis = System.currentTimeMillis();
        Date agora = new Date(agoraMillis);
        Date validade = new Date(agoraMillis + 3600000); // 1 hora de expiração

        return Jwts.builder()
                .subject(clientId)
                .issuedAt(agora)
                .expiration(validade)
                .signWith(key)
                .compact();
    }

    public boolean validarToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token inválido, expirado ou com assinatura adulterada
            return false;
        }
    }
}