package com.projet.projet_RoadMoto.dto.etapedto;

import com.projet.projet_RoadMoto.domain.TypeEtape;

import java.time.OffsetDateTime;

public record EtapeResponse(int ordre,
                            TypeEtape type,
                            double longitude,
                            double latitude,
                            Long poiId,
                            String poiNom,
                            Double distanceDepuisPrecedenteKm,
                            Integer dureeDepuisPrecedenteMin,
                            Integer dureeArretMin,
                            OffsetDateTime heurePassage) {
}
