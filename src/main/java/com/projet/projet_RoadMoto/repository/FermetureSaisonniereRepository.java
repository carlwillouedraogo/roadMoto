package com.projet.projet_RoadMoto.repository;


import com.projet.projet_RoadMoto.entity.FermetureSaisonniere;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FermetureSaisonniereRepository extends JpaRepository<FermetureSaisonniere, Long> {
    Optional<FermetureSaisonniere> findById(Long id);
}
