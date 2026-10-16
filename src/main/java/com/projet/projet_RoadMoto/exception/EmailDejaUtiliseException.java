package com.projet.projet_RoadMoto.exception;

public class EmailDejaUtiliseException extends RuntimeException {
    public EmailDejaUtiliseException(String email) {
        super("L'email " + email + " est déjà utilisé");
    }
}
