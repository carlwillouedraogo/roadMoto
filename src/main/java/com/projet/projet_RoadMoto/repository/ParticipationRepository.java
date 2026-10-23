package com.projet.projet_RoadMoto.repository;

import com.projet.projet_RoadMoto.domain.StatutParticipation;
import com.projet.projet_RoadMoto.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    boolean existsBySortieIdAndUtilisateurId(Long id, Long uId);
    int countBySortieIdAndStatut(Long id, StatutParticipation statutParticipation);
}
