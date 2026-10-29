package com.projet.projet_RoadMoto.repository;

import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.domain.StatutParticipation;
import com.projet.projet_RoadMoto.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    int countBySortieIdAndStatut(Long id, StatutParticipation statutParticipation);
    Optional<Participation> findBySortieIdAndUtilisateurId(Long sortieId, Long utilisateurId);
}
