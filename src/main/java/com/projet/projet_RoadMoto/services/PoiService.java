package com.projet.projet_RoadMoto.services;

import com.projet.projet_RoadMoto.domain.TypePoi;
import com.projet.projet_RoadMoto.dto.PoiResponse;
import com.projet.projet_RoadMoto.entity.Poi;
import com.projet.projet_RoadMoto.repository.PoiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lecture des points d'intérêt (cols, restaurants, stations...) pour l'API.
 * L'import depuis OpenStreetMap est géré à part, dans OverpassImportService.
 */
@Service
public class PoiService {
    private final PoiRepository poiRepository;

    public PoiService(PoiRepository poiRepository) {
        this.poiRepository = poiRepository;
    }

    // POI situés à moins de rayonMetres du point (lng, lat), du plus proche au plus loin.
    // type null = tous les types ; il est passé en String car la requête est native (SQL PostGIS)
    @Transactional(readOnly = true)
    public List<PoiResponse> aProximite(double lng, double lat, int rayonMetres, TypePoi type){
        return poiRepository.aProximite(lng,lat,rayonMetres,type != null ? type.name() : null)
                .stream()
                .map(this::poiResponse)
                .toList();
    }

    // Attention à l'ordre : en JTS, Y = latitude et X = longitude
    private PoiResponse poiResponse(Poi poi){
        return new PoiResponse(poi.getId(), poi.getNom(), poi.getType(),
                poi.getLocalisation().getY(), poi.getLocalisation().getX(),
                poi.getAltitudeMetres());
    }

}
