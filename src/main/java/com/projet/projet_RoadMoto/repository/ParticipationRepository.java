package com.projet.projet_RoadMoto.repository;

import com.projet.projet_RoadMoto.domain.StatutParticipation;
import com.projet.projet_RoadMoto.entity.Participation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    int countBySortieIdAndStatut(Long id, StatutParticipation statutParticipation);
    Optional<Participation> findBySortieIdAndUtilisateurId(Long sortieId, Long utilisateurId);

    @EntityGraph(attributePaths = {"sortie", "sortie.organisateur"})
    @Query("""
    select p from Participation p
    where p.utilisateur.id = :utilisateurId
      and p.statut = coalesce(:statut, p.statut)
    order by p.sortie.dateDepart desc
    """)
    Page<Participation> historique(@Param("utilisateurId") Long utilisateurId,
                                   @Param("statut") StatutParticipation statut,
                                   Pageable pageable);
}
