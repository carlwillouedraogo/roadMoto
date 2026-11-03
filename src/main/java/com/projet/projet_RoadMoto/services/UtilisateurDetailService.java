package com.projet.projet_RoadMoto.services;

import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;
import com.projet.projet_RoadMoto.security.UtilisateurPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Pont entre Spring Security et nos utilisateurs : Spring Security l'appelle pour charger
 * un utilisateur à partir de son identifiant de connexion, ici l'email.
 */
@Service
public class UtilisateurDetailService implements UserDetailsService {
    private final UtilisateurRepository repository;

    public UtilisateurDetailService(UtilisateurRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur u = repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));

        // UtilisateurPrincipal adapte notre entité Utilisateur à l'interface UserDetails de Spring Security
        return new UtilisateurPrincipal(u);
    }
}