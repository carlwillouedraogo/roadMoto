package com.projet.projet_RoadMoto.repository;

import com.projet.projet_RoadMoto.entity.Itineraire;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ItineraireRepository extends JpaRepository<Itineraire,Long> {
    @EntityGraph(attributePaths = {"etapes", "etapes.poi"})
    Optional<Itineraire> findWithEtapesBySortieId(Long sortieId);

    // suppression en SQL direct : les étapes partent via ON DELETE CASCADE,
    // ce qui évite le conflit sur uk_etape_ordre lors d'un recalcul
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "delete from itineraire where sortie_id = :sortieId", nativeQuery = true)
    void supprimerParSortieId(@Param("sortieId") Long sortieId);

    @EntityGraph(attributePaths = {"sortie", "etapes", "etapes.poi"})
    Optional<Itineraire> findWithEtapesById (Long sortieId);
}
