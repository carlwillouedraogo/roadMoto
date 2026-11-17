package com.projet.projet_RoadMoto.filter;

import com.projet.projet_RoadMoto.services.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Pas d'en-tête, ou pas au format Bearer : on laisse passer sans authentifier.
        // La route est peut-être publique — ce n'est pas au filtre d'en décider.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);   // longueur de "Bearer "

        try {
            final String email = jwtService.extractEmail(token);

            // Si le contexte est déjà rempli, un autre filtre a fait le travail :
            // inutile de recharger l'utilisateur depuis la base.
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails utilisateur = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, utilisateur)) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    utilisateur,
                                    null,                          // pas de credentials : le token les remplace
                                    utilisateur.getAuthorities()
                            );
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (JwtException | UsernameNotFoundException e) {
            // Token invalide, expiré, signé avec une autre clé, utilisateur supprimé :
            // on n'authentifie pas, et Spring Security répondra 401.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
