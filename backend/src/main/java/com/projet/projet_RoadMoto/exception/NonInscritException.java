package com.projet.projet_RoadMoto.exception;

public class NonInscritException extends RuntimeException {
    public NonInscritException(String message) {
        super(String.valueOf(message));
    }
}
