package com.projet.projet_RoadMoto.exception;

public class DejaInscritException extends RuntimeException {
    public DejaInscritException(Long message) {
        super(String.valueOf(message));
    }
}
