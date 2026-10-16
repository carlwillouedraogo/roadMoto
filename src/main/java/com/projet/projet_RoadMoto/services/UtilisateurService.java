package com.projet.projet_RoadMoto.services;

import com.projet.projet_RoadMoto.dto.InscriptionRequest;
import com.projet.projet_RoadMoto.domain.Role;
import com.projet.projet_RoadMoto.dto.UtilisateurResponse;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;
import com.projet.projet_RoadMoto.exception.EmailDejaUtiliseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;

@Service
public class UtilisateurService {
    private final UtilisateurRepository repository;
    private final PasswordEncoder encoder;

    public UtilisateurService(UtilisateurRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
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
        return new UtilisateurResponse(
                sauvegarde.getId(),
                sauvegarde.getEmail(),
                sauvegarde.getPseudo(),
                sauvegarde.getNiveau()
        );
    }
}
