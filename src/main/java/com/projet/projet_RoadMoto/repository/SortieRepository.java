package com.projet.projet_RoadMoto.repository;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.entity.Sortie;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SortieRepository extends JpaRepository<Sortie, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sortie s where s.id = :id")
    Optional<Sortie> findByIdPourInscription(@Param("id") Long id);

    @EntityGraph(attributePaths = "organisateur")
    @Query("""
    select s from Sortie s
    where s.statut = com.projet.projet_RoadMoto.domain.Statut.PUBLIEE
      and s.dateDepart > :maintenant
      and s.niveau     = coalesce(:niveau, s.niveau)
      and s.dateDepart <= coalesce(:dateMax, s.dateDepart)
      and s.placesMax  >= coalesce(:placesMin, s.placesMax)
    """)
    Page<Sortie> rechercher(@Param("maintenant") OffsetDateTime maintenant,
                            @Param("niveau") Niveau niveau,
                            @Param("dateMax") OffsetDateTime dateMax,
                            @Param("placesMin") Integer placesMin,
                            Pageable pageable);
}
