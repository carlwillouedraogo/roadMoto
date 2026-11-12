package com.projet.projet_RoadMoto.exception;

/** Les étapes demandées ne permettent pas de tracer une route (ex. deux étapes au même endroit). */
public class EtapesInvalidesException extends RuntimeException {
    public EtapesInvalidesException(String message) {
        super(message);
    }
}
