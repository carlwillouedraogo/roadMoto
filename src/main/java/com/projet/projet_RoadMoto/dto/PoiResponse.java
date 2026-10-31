package com.projet.projet_RoadMoto.dto;

import com.projet.projet_RoadMoto.domain.TypePoi;

public record PoiResponse(Long id, String nom, TypePoi type,
                          double latitude, double longitude,
                          Integer altitudeMetres) {
}
