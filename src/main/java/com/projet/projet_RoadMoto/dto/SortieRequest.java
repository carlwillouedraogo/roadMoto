package com.projet.projet_RoadMoto.dto;


import com.projet.projet_RoadMoto.domain.Niveau;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;

public record SortieRequest(
       @Schema (example = "Tour du Léman") @NotBlank @Size(max = 120) String titre,
        @Size(max = 2000) String description,
        @Schema (example = "2026-11-15T09:00:00+01:00") @NotNull @Future OffsetDateTime dateDepart,
        @Schema (example = "Genève") @NotBlank String nomLieuDepart,
        @Schema (example = "6.1432") @NotNull Double latitude,
        @Schema (example = "46.2044") @NotNull Double longitude,
        @Schema (example = "DECOUVERTE") @NotNull Niveau niveau,
        @Min(2) @Max(50) int placesMax) {
}
