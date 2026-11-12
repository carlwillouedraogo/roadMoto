package com.projet.projet_RoadMoto.dto.poidto;

public record FermetureReference(long osmId, String nom,
                                 Boolean ouvertToutAnnee,
                                 Integer ouvertureDebut, Integer ouvertureFin) {
}
