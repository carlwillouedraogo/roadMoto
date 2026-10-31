package com.projet.projet_RoadMoto.controller;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.domain.TypePoi;
import com.projet.projet_RoadMoto.dto.PoiResponse;
import com.projet.projet_RoadMoto.services.PoiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/poi")
public class PoiController {
    private final PoiService poiService;

    public PoiController(PoiService poiService) {
        this.poiService = poiService;
    }

    @GetMapping
    public List<PoiResponse> getPoisAProximite(@RequestParam(required = false) double lng,
                                     @RequestParam(required = false) double lat,
                                     @RequestParam(required = false) Integer rayonMetres,
                                     @RequestParam(required = false) TypePoi typePoi){
        return poiService.aProximite(lng,lat,rayonMetres,typePoi);
    }
}
