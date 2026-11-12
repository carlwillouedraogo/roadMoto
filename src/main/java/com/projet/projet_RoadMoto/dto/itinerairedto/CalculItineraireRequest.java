package com.projet.projet_RoadMoto.dto.itinerairedto;

import com.projet.projet_RoadMoto.dto.etapedto.EtapeRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Étapes ordonnées de l'itinéraire, départ et arrivée compris.
 * ORS limite le nombre de points par requête (50 sur l'offre gratuite).
 */
// Exemple complet : sans lui, Swagger répète minItems (2) fois le même exemple d'étape,
// ce qui donne deux étapes identiques, refusées à l'appel.
@Schema(example = """
        {
          "etapes": [
            { "longitude": 7.0717, "latitude": 46.1028, "type": "DEPART" },
            { "poiId": 774, "type": "INTERMEDIAIRE", "dureeArretMin": 15 },
            { "longitude": 7.3606, "latitude": 46.2331, "type": "ARRIVEE" }
          ]
        }
        """)
public record CalculItineraireRequest(@NotNull @Size(min = 2, max = 50) List<@Valid @NotNull EtapeRequest> etapes) {
}
