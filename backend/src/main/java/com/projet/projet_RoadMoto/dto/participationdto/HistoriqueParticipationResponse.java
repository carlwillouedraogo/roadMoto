package com.projet.projet_RoadMoto.dto.participationdto;

import com.projet.projet_RoadMoto.domain.StatutParticipation;
import com.projet.projet_RoadMoto.dto.sortiedto.SortieResume;

import java.time.OffsetDateTime;

public record HistoriqueParticipationResponse(Long id,
                                              StatutParticipation statut,
                                              OffsetDateTime inscritLe,
                                              SortieResume sortie) {
}
