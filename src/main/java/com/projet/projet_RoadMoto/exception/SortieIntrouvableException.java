package com.projet.projet_RoadMoto.exception;

public class SortieIntrouvableException extends RuntimeException {
    public SortieIntrouvableException(Long message) {
        super(String.valueOf(message));
    }
}
