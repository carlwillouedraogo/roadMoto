package com.projet.projet_RoadMoto.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    // Clé secrète injectée depuis application.yml
    private final SecretKey secretKey;

    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secretKey,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
        this.expirationMs = expirationMs;
    }

    // Génère un token avec des claims personnalisés
    public String generateToken(Map<String, Object> extractClaims, UserDetails userDetails){
        return Jwts.builder()
                .claims(extractClaims) // Claims additionnels (rôles, permissions)
                .subject(userDetails.getUsername()) // Identifiant principal
                .issuedAt(new Date()) // Date de création
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey, Jwts.SIG.HS256) // Signature HMAC-SHA256
                .compact();
    }

    // Génère un token JWT pour un utilisateur authentifié
    public String generateTokens(UserDetails u) {
        return generateToken(new HashMap<>(), u);
    }

    // Extrait un claim spécifique via une fonction d'extraction
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Extrait l'email (subject) du token
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Valide le token : signature correcte et non expiré
    public boolean isTokenValid(String token, UserDetails user) {
        final String username = extractEmail(token);
        return username.equals(user.getUsername()) && !isTokenExpired(token);
    }

    // Vérifie si le token a expiré
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Extrait la date d'expiration
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Parse le token et extrait tous les claims
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)    // Vérifie la signature
                .build()
                .parseSignedClaims(token)       // Parse le token signé
                .getPayload();                  // Retourne les claims
    }



}
