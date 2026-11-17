package com.projet.projet_RoadMoto.repository;


import com.projet.projet_RoadMoto.entity.FermetureSaisonniere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FermetureSaisonniereRepository extends JpaRepository<FermetureSaisonniere, Long> {
    Optional<FermetureSaisonniere> findById(Long id);

    @Query(value = """
    select f.* from fermeture_saisonniere f
    join poi p        on p.id = f.poi_id
    join itineraire i on i.sortie_id = :sortieId
    where ST_DWithin(p.localisation, i.trace, :toleranceMetres)
    """, nativeQuery = true)
    List<FermetureSaisonniere> colsTraverses(@Param("sortieId") Long sortieId,
                                             @Param("toleranceMetres") int toleranceMetres);
}
