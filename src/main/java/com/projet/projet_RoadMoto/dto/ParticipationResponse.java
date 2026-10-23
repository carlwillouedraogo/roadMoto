package com.projet.projet_RoadMoto.dto;

import com.projet.projet_RoadMoto.domain.StatutParticipation;

import java.time.OffsetDateTime;

public record ParticipationResponse(Long id,
                                    Long sortieId,
                                    UtilisateurResume participant,
                                    StatutParticipation statut,
                                    OffsetDateTime inscritLe,
                                    int placesRestantes) {
}
