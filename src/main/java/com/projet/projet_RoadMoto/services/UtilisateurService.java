package com.projet.projet_RoadMoto.services;

import com.projet.projet_RoadMoto.dto.InscriptionRequest;
import com.projet.projet_RoadMoto.domain.Role;
import com.projet.projet_RoadMoto.dto.LoginRequest;
import com.projet.projet_RoadMoto.dto.TokenResponse;
import com.projet.projet_RoadMoto.dto.UtilisateurResponse;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;
import com.projet.projet_RoadMoto.exception.EmailDejaUtiliseException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UtilisateurService {
    private final UtilisateurRepository repository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;


    public UtilisateurService(UtilisateurRepository repository, PasswordEncoder encoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.repository = repository;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Méthode permettant d'ajouter un nouvel utilisateur
     * @param request
     * @return UtilisateurResponse
     */
    @Transactional
    public UtilisateurResponse inscrire(InscriptionRequest request){
        if(repository.existsByEmail(request.email())){
            throw new EmailDejaUtiliseException(request.email());
        }
        Utilisateur u = new Utilisateur();
        u.setEmail(request.email());
        u.setMotDePasseHash(encoder.encode(request.motDePasse()));
        u.setPseudo(request.pseudo());
        u.setMotoModele(request.motoModele());
        u.setRole(Role.USER);
        u.setNiveau(request.niveau());
        u.setCreeLe(OffsetDateTime.now());
        Utilisateur sauvegarde = repository.save(u);
        return toResponse(sauvegarde);
    }

    public TokenResponse authenticate(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.motDePasse()
                )
        );
        UserDetails utilisateurDetails = (UserDetails) auth.getPrincipal();
        Map<String, Object> claims = new HashMap<>();
        claims.put("Role", utilisateurDetails.getAuthorities());

        String jwtToken = jwtService.generateToken(claims, utilisateurDetails);
        return new TokenResponse(jwtToken, jwtService.getExpirationMs());
    }

    @Transactional(readOnly = true)
    public UtilisateurResponse byEmail(String email){
        Utilisateur u = repository.findByEmail(email)
                .orElseThrow(() -> new EmailDejaUtiliseException(email));
        return toResponse(u);
    }


    public UtilisateurResponse toResponse(Utilisateur u) {
        return new UtilisateurResponse(u.getId(), u.getEmail(), u.getPseudo(), u.getNiveau());
    }

}
