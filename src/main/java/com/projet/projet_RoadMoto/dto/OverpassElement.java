package com.projet.projet_RoadMoto.dto;

import java.util.Map;

public record OverpassElement(Long id, double lat, double lon, Map<String,String> tags) {
}
