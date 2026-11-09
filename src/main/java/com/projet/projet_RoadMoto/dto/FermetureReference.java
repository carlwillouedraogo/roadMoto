package com.projet.projet_RoadMoto.dto;

public record FermetureReference(long osmId, String nom,
                                 Boolean ouvertToutAnnee,
                                 Integer ouvertureDebut, Integer ouvertureFin) {
}
