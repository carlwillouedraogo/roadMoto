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

class JwtServiceTest {

    private String secret;
    private JwtService jwtService;
    private UserDetails utilisateur;

    @BeforeEach
    void setUp() {
        secret = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
        jwtService = new JwtService(secret, 3_600_000L);
        utilisateur = User.builder()
                .username("carl@example.com").password("peu-importe")
                .authorities("ROLE_USER").build();
    }

    @Test
    void genere_un_token_relisible() {
        String token = jwtService.generateTokens(utilisateur);

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
        assertThat(jwtService.extractEmail(token)).isEqualTo("carl@example.com");
    }

    @Test
    void valide_le_token_de_son_proprietaire() {
        String token = jwtService.generateTokens(utilisateur);
        assertThat(jwtService.isTokenValid(token, utilisateur)).isTrue();
    }

    @Test
    void refuse_le_token_d_un_autre_utilisateur() {
        String token = jwtService.generateTokens(utilisateur);
        UserDetails autre = User.builder()
                .username("autre@example.com").password("x").authorities("ROLE_USER").build();

        assertThat(jwtService.isTokenValid(token, autre)).isFalse();
    }

    @Test
    void refuse_un_token_expire() {
        JwtService jwtExpire = new JwtService(secret, -1000L);
        String token = jwtExpire.generateTokens(utilisateur);

        assertThatThrownBy(() -> jwtExpire.isTokenValid(token, utilisateur))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void refuse_un_token_signe_avec_une_autre_cle() {
        String autreSecret = Encoders.BASE64.encode(
                Jwts.SIG.HS256.key().build().getEncoded());
        String tokenEtranger = new JwtService(autreSecret, 3_600_000L)
                .generateTokens(utilisateur);

        assertThatThrownBy(() -> jwtService.extractEmail(tokenEtranger))
                .isInstanceOf(SignatureException.class);
    }
}
