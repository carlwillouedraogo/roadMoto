package com.projet.projet_RoadMoto;


import com.projet.projet_RoadMoto.services.JwtService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.security.SignatureException;

import static org.assertj.core.api.Assertions.*;

/**
 * Test unitaire pur (pas de contexte Spring) : JwtService est instancié à la main
 * avec une clé HS256 générée aléatoirement pour chaque test.
 */
class JwtServiceTest {

    private String secret;
    private JwtService jwtService;
    private UserDetails utilisateur;

    @BeforeEach
    void setUp() {
        // clé HMAC-SHA256 aléatoire, encodée en Base64 comme dans la configuration de l'appli
        secret = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
        jwtService = new JwtService(secret, 3_600_000L);   // durée de validité : 1 h
        utilisateur = User.builder()
                .username("carl@example.com").password("peu-importe")
                .authorities("ROLE_USER").build();
    }

    @Test
    void genere_un_token_relisible() {
        String token = jwtService.generateTokens(utilisateur);

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);   // format JWT : header.payload.signature
        assertThat(jwtService.extractEmail(token)).isEqualTo("carl@example.com");
    }

    @Test
    void valide_le_token_de_son_proprietaire() {
        String token = jwtService.generateTokens(utilisateur);
        assertThat(jwtService.isTokenValid(token, utilisateur)).isTrue();
    }

    @Test
    void refuse_le_token_d_un_autre_utilisateur() {
        // token valide, mais émis pour un autre email que celui de l'utilisateur vérifié
        String token = jwtService.generateTokens(utilisateur);
        UserDetails autre = User.builder()
                .username("autre@example.com").password("x").authorities("ROLE_USER").build();

        assertThat(jwtService.isTokenValid(token, autre)).isFalse();
    }

    @Test
    void refuse_un_token_expire() {
        // durée négative : le token est déjà expiré au moment où il est généré
        JwtService jwtExpire = new JwtService(secret, -1000L);
        String token = jwtExpire.generateTokens(utilisateur);

        assertThatThrownBy(() -> jwtExpire.isTokenValid(token, utilisateur))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void refuse_un_token_signe_avec_une_autre_cle() {
        // token produit par un autre service avec sa propre clé : la signature ne doit pas être acceptée
        String autreSecret = Encoders.BASE64.encode(
                Jwts.SIG.HS256.key().build().getEncoded());
        String tokenEtranger = new JwtService(autreSecret, 3_600_000L)
                .generateTokens(utilisateur);

        assertThatThrownBy(() -> jwtService.extractEmail(tokenEtranger))
                .isInstanceOf(SignatureException.class);
    }
}
