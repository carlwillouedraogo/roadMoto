package com.projet.projet_RoadMoto.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public record LoginRequest(
        @Schema(example = "felix@example.com") @NotBlank @Email String email,
        @Schema(example = "motdepasse123") @NotBlank String motDePasse) {}


