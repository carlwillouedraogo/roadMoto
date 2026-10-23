package com.projet.projet_RoadMoto.dto;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.domain.Statut;

import java.time.OffsetDateTime;

public record SortieResume(Long id,
                           String titre,
                           String description,
                           OffsetDateTime dateDepart,
                           String nomLieuDepart,
                           Double latitude,
                           Double longitude,
                           Niveau niveau,
                           Statut statut,
                           int placesMax,
                           UtilisateurResume organisateur,
                           OffsetDateTime creeLe) {
}
