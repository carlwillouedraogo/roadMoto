package com.projet.projet_RoadMoto.exception;

public class ItineraireIncalculableException extends RuntimeException {
    public ItineraireIncalculableException(String message) {
        super(message);
    }

    public ItineraireIncalculableException(String message, Throwable cause) {
        super(message, cause);
    }
}
