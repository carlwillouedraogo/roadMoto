package com.projet.projet_RoadMoto.dto;

import com.projet.projet_RoadMoto.entity.Poi;

public record AvertissementResponse(Long poiId,
                                    String nom,
                                    Integer altitude,
                                    Integer moisOuvertureDebut,
                                    Integer moisOuvertureFin,
                                    String message) {
}
