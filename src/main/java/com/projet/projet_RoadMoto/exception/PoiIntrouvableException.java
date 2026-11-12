package com.projet.projet_RoadMoto.exception;

public class PoiIntrouvableException extends RuntimeException {
    public PoiIntrouvableException(Long poiId) {
        super("POI " + poiId + " introuvable");
    }
}
