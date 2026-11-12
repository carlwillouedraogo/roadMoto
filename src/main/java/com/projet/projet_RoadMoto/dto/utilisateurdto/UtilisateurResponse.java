package com.projet.projet_RoadMoto.dto.utilisateurdto;

import com.projet.projet_RoadMoto.domain.Niveau;

public record UtilisateurResponse(Long id, String email, String pseudo, Niveau niveau)
{}
