package com.projet.projet_RoadMoto.dto.itinerairedto;

import com.projet.projet_RoadMoto.dto.etapedto.TronconCalcule;
import org.locationtech.jts.geom.LineString;

import java.util.List;

public record ItineraireCalcule(LineString trace, int distanceM, int dureeS, List<TronconCalcule> troncons) {
}
