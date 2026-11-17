package com.projet.projet_RoadMoto.repository;

import com.projet.projet_RoadMoto.entity.Poi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OverpassImportRepository extends JpaRepository<Poi, Long> {
    Optional<Poi> findByOsmId(Long osmId);
}
