package com.projet.projet_RoadMoto.exception;

import com.projet.projet_RoadMoto.domain.Statut;

public class TransitionInvalideException extends RuntimeException {
    public TransitionInvalideException(Statut message,Statut statut) {
        super("ce statut " + message + " correspond à ce statut " + statut);

    }
}
