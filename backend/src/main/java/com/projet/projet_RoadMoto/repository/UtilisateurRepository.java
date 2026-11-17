package com.projet.projet_RoadMoto.repository;

import com.projet.projet_RoadMoto.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur,Long> {
    boolean existsByEmail(String email);
    Optional<Utilisateur> findByEmail(String email);
}
