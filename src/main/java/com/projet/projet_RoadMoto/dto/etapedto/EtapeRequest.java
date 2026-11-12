package com.projet.projet_RoadMoto.dto.etapedto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.projet.projet_RoadMoto.domain.TypeEtape;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Une étape demandée : soit un POI existant (poiId), soit un point libre (longitude + latitude).
 */
public record EtapeRequest(
        @Schema(example = "774") Long poiId,
        @Schema(example = "7.0013461")
        @DecimalMin(value = "-180.0", message = "Longitude invalide")
        @DecimalMax(value = "180.0", message = "Longitude invalide")
        Double longitude,
        @Schema(example = "46.0577309")
        @DecimalMin(value = "-90.0", message = "Latitude invalide")
        @DecimalMax(value = "90.0", message = "Latitude invalide")
        Double latitude,
        @Schema(example = "DEPART") @NotNull TypeEtape type,
        @Schema(example = "15") @PositiveOrZero Integer dureeArretMin) {

    // règle de validation, pas un champ du JSON : masquée pour Jackson et Swagger
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Une étape doit référencer un POI ou fournir longitude et latitude")
    public boolean isPositionRenseignee() {
        return poiId != null || (longitude != null && latitude != null);
    }
}
