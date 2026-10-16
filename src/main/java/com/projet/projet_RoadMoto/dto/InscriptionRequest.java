package com.projet.projet_RoadMoto.dto;

import com.projet.projet_RoadMoto.domain.Niveau;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InscriptionRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String motDePasse,
        @NotBlank @Size(min = 3, max = 50) String pseudo,
        String motoModele,
        @NotNull Niveau niveau
) {}
