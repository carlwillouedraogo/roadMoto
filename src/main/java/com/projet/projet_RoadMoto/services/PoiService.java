package com.projet.projet_RoadMoto.services;

import com.projet.projet_RoadMoto.domain.TypePoi;
import com.projet.projet_RoadMoto.dto.PoiResponse;
import com.projet.projet_RoadMoto.entity.Poi;
import com.projet.projet_RoadMoto.repository.PoiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PoiService {
    private final PoiRepository poiRepository;

    public PoiService(PoiRepository poiRepository) {
        this.poiRepository = poiRepository;
    }

    @Transactional(readOnly = true)
    public List<PoiResponse> aProximite(double lng, double lat, int rayonMetres, TypePoi type){
        return poiRepository.aProximite(lng,lat,rayonMetres,type != null ? type.name() : null)
                .stream()
                .map(this::poiResponse)
                .toList();
    }

    private PoiResponse poiResponse(Poi poi){
        return new PoiResponse(poi.getId(), poi.getNom(), poi.getType(),
                poi.getLocalisation().getY(), poi.getLocalisation().getX(),
                poi.getAltitudeMetres());
    }
}
