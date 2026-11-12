package com.projet.projet_RoadMoto.exception;

public class ItineraireIntrouvableException extends RuntimeException {
    public ItineraireIntrouvableException(Long sortieId) {
        super("Aucun itinéraire pour la sortie " + sortieId);
    }
}
