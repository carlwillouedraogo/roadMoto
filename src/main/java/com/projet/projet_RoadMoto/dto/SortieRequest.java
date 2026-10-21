package com.projet.projet_RoadMoto.dto;


import com.projet.projet_RoadMoto.domain.Niveau;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;

public record SortieRequest(@NotBlank @Size(max = 120) String titre,
                            @Size(max = 2000) String description,
                            @NotNull @Future OffsetDateTime dateDepart,
                            @NotBlank String nomLieuDepart,
                            @NotNull Double latitude,
                            @NotNull Double longitude,
                            @NotNull Niveau niveau,
                            @Min(2) @Max(50) int placesMax) {
}
