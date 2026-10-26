package com.projet.projet_RoadMoto.dto;

import com.projet.projet_RoadMoto.domain.Niveau;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InscriptionRequest(
        @Schema(example = "felix@example.com") @NotBlank @Email String email,
        @Schema(example = "motdepasse123") @NotBlank @Size(min = 8) String motDePasse,
        @Schema(example = "felix")  @NotBlank @Size(min = 3, max = 50) String pseudo,
        @Schema(example = "Yamaha tracer 7") String motoModele,
        @Schema(example = "TOURING") @NotNull Niveau niveau
) {}
