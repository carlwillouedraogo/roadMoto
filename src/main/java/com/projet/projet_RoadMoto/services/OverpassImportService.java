package com.projet.projet_RoadMoto.services;

import com.projet.projet_RoadMoto.domain.Source;
import com.projet.projet_RoadMoto.domain.TypePoi;
import com.projet.projet_RoadMoto.dto.poidto.OverpassElement;
import com.projet.projet_RoadMoto.dto.poidto.OverpassResponse;
import com.projet.projet_RoadMoto.domain.ResultatImport;
import com.projet.projet_RoadMoto.entity.Poi;
import com.projet.projet_RoadMoto.repository.OverpassImportRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

@Service
public class OverpassImportService {

    private static final Logger log = LoggerFactory.getLogger(OverpassImportService.class);

    private final OverpassImportRepository overpassImportRepository;
    private final String overpassUrl;

    private final RestClient client;

    private static final GeometryFactory GEO =
            new GeometryFactory(new PrecisionModel(), 4326);

    public OverpassImportService(OverpassImportRepository overpassImportRepository,
                                 RestClient.Builder builder,
                                 @Value("${app.overpass.url}") String overpassUrl) {
        this.overpassImportRepository = overpassImportRepository;
        this.overpassUrl = overpassUrl;
        this.client = builder
                .defaultHeader("User-Agent", "RoadMoto/0.1 (projet portfolio)")
                .build();
    }

    // Requête Overpass QL : cols du Valais (CH-VS) situés sur une route carrossable,
    // pour écarter les cols uniquement accessibles à pied
    private static final String REQUETE_COLS_VALAIS = """
    [out:json][timeout:120];
    area["ISO3166-2"="CH-VS"]->.vs;
    node(area.vs)["mountain_pass"="yes"]->.cols;
    way(bn.cols)["highway"~"^(trunk|primary|secondary|tertiary|unclassified)$"]->.routes;
    node.cols(w.routes);
    out body;
    """;

    @Transactional
    public ResultatImport importerColsValais() {
        OverpassResponse reponse = interroger(REQUETE_COLS_VALAIS);

        int crees = 0, misAJour = 0, ignores = 0;
        if (reponse == null || reponse.elements() == null) {
            return new ResultatImport(crees, misAJour, ignores);
        }

        // Upsert par osmId : un élément déjà importé est mis à jour au lieu d'être dupliqué
        for (OverpassElement e : reponse.elements()) {
            if (e.tags() == null || e.tags().get("name") == null) {
                ignores++;
                continue;
            }
            Optional<TypePoi> type = typeDepuisTags(e.tags());
            if (type.isEmpty()) {
                ignores++;
                continue;
            }
            Optional<Poi> existant = overpassImportRepository.findByOsmId(e.id());
            overpassImportRepository.save(existant.map(p -> mettreAJour(p, e, type.get()))
                    .orElseGet(() -> creer(e, type.get())));
            if (existant.isPresent()) misAJour++;
            else crees++;
        }

        log.info("Import Valais : {} créés, {} mis à jour, {} ignorés", crees, misAJour, ignores);
        return new ResultatImport(crees, misAJour, ignores);
    }


    // Le tag "ele" d'OSM est du texte libre ("2469", "1 950 m"...) :
    // on garde uniquement chiffres, point et signe, et on renvoie null si ce n'est pas lisible
    private Integer parseAltitude(String ele) {
        if (ele == null) return null;
        try {
            return (int) Double.parseDouble(ele.replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Poi creer(OverpassElement e, TypePoi type) {
        Poi poi = new Poi();
        poi.setOsmId(e.id());
        poi.setSource(Source.OSM);
        return appliquer(poi, e, type);
    }

    private Poi mettreAJour(Poi existant, OverpassElement e, TypePoi type) {
        return appliquer(existant, e, type);
    }

    private Poi appliquer(Poi poi, OverpassElement e, TypePoi type) {
        poi.setNom(e.tags().get("name"));
        poi.setType(type);
        poi.setLocalisation(GEO.createPoint(new Coordinate(e.lon(), e.lat())));
        poi.setAltitudeMetres(parseAltitude(e.tags().get("ele")));
        return poi;
    }

    // Overpass attend la requête QL brute dans le corps d'un POST
    private OverpassResponse interroger(String requete) {
        return client.post()
                .uri(overpassUrl)
                .body(requete)
                .retrieve()
                .body(OverpassResponse.class);
    }

    // Optional.empty() si aucun tag reconnu : la colonne type est NOT NULL, l'élément doit être ignoré
    private Optional<TypePoi> typeDepuisTags(Map<String, String> tags) {
        if ("yes".equals(tags.get("mountain_pass")) || "saddle".equals(tags.get("natural")))
            return Optional.of(TypePoi.COL);
        if ("viewpoint".equals(tags.get("tourism")))
            return Optional.of(TypePoi.POINT_DE_VUE);

        String amenity = tags.get("amenity");
        if (amenity == null) return Optional.empty();
        return switch (amenity) {
            case "restaurant" -> Optional.of(TypePoi.RESTAURANT);
            case "cafe", "bar", "pub" -> Optional.of(TypePoi.BISTROT);
            case "fuel" -> Optional.of(TypePoi.STATION);
            case "parking", "motorcycle_parking" -> Optional.of(TypePoi.PARKING);
            default -> Optional.empty();
        };
    }
}
