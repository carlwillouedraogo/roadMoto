package com.projet.projet_RoadMoto.services;

import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.exception.EmailDejaUtiliseException;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UtilisateurDetailService implements UserDetailsService {
    private final UtilisateurRepository repository;

    public UtilisateurDetailService(UtilisateurRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur u = repository.findByEmail(email)
                .orElseThrow(() -> new EmailDejaUtiliseException(email));

        return User.builder()
                .username(u.getEmail())
                .password(u.getMotDePasseHash())
                .authorities("ROLE_" + u.getRole().name())
                .build();
    }
}
