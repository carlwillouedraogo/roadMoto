package com.projet.projet_RoadMoto.exception;

import com.projet.projet_RoadMoto.domain.Statut;

public class TransitionInvalideException extends RuntimeException {
    public TransitionInvalideException(Statut message,Statut statut) {
        super("Transition de " + message + " à " + statut + " impossible");
    }
}
