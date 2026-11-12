package com.projet.projet_RoadMoto.dto.itinerairedto;

import com.projet.projet_RoadMoto.domain.Source;
import com.projet.projet_RoadMoto.dto.etapedto.EtapeResponse;

import java.time.OffsetDateTime;
import java.util.List;

public record ItineraireResponse(Long sortieId,
                                 double distanceKm,
                                 int dureeMin,
                                 List<List<Double>> trace,
                                 List<EtapeResponse> etapes,
                                 Source source,
                                 OffsetDateTime calculeLe) {
}
