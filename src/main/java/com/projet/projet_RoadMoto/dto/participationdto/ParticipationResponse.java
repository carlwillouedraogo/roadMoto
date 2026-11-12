package com.projet.projet_RoadMoto.dto.participationdto;

import com.projet.projet_RoadMoto.domain.StatutParticipation;
import com.projet.projet_RoadMoto.dto.utilisateurdto.UtilisateurResume;

import java.time.OffsetDateTime;

public record ParticipationResponse(Long id,
                                    Long sortieId,
                                    UtilisateurResume participant,
                                    StatutParticipation statut,
                                    OffsetDateTime inscritLe,
                                    int placesRestantes) {
}
