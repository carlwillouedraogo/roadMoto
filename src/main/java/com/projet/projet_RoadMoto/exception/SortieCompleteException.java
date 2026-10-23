package com.projet.projet_RoadMoto.exception;

public class SortieCompleteException extends RuntimeException {
    public SortieCompleteException(Long message) {
        super(String.valueOf(message));
    }
}
