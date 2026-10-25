package com.projet.projet_RoadMoto.repository;

import com.projet.projet_RoadMoto.entity.Sortie;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SortieRepository extends JpaRepository<Sortie, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sortie s where s.id = :id")
    Optional<Sortie> findByIdPourInscription(@Param("id") Long id);
}
