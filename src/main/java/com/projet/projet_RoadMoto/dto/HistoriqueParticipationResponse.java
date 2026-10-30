package com.projet.projet_RoadMoto.dto;

import com.projet.projet_RoadMoto.domain.StatutParticipation;

import java.time.OffsetDateTime;

public record HistoriqueParticipationResponse(Long id,
                                              StatutParticipation statut,
                                              OffsetDateTime inscritLe,
                                              SortieResume sortie) {
}
