package com.projet.projet_RoadMoto;

import com.projet.projet_RoadMoto.domain.Source;
import com.projet.projet_RoadMoto.domain.TypePoi;
import com.projet.projet_RoadMoto.dto.PoiResponse;
import com.projet.projet_RoadMoto.entity.Poi;
import com.projet.projet_RoadMoto.repository.PoiRepository;
import com.projet.projet_RoadMoto.services.PoiService;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Teste la recherche de POI par proximité (requête PostGIS ST_DWithin) sur une vraie base PostGIS.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({TestcontainersConfig.class, PoiService.class})
public class PoiRepositoryTest {

    private static final GeometryFactory GEO =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private PoiRepository poiRepository;

    @Autowired
    private PoiService poiService;

    @Autowired
    private TestEntityManager em;

    @Test
    void testPoiAProximite(){
        Poi geneve = poiRepository.save(createPoi("Genève",TypePoi.STATION,6.1432,46.2044,375));
        Poi carouge = poiRepository.save(createPoi("Carouge",TypePoi.STATION,6.1393,46.1840,382));
        poiRepository.save(createPoi("Restaurant Genève",TypePoi.RESTAURANT,6.1450,46.2050,375));
        poiRepository.save(createPoi("Lausanne",TypePoi.STATION,6.6323,46.5197,495));
        // flush + clear : la requête native doit lire les données en base, pas le cache JPA
        em.flush();
        em.clear();

        // rayon de 5 km autour de Genève : Lausanne (~50 km) est exclue, le restaurant est filtré par le type
        List<PoiResponse> listPoi = poiService.aProximite(geneve.getLocalisation().getX(),geneve.getLocalisation().getY(),5000,TypePoi.STATION);

        assertThat(listPoi)
                .extracting(PoiResponse::id)
                .containsExactly(geneve.getId(), carouge.getId());   // triés du plus proche au plus loin

    }

    private Poi createPoi(String nom, TypePoi typePoi, double lng, double lat, Integer altMetres){
        Poi poi = new Poi();
        poi.setNom(nom);
        poi.setType(typePoi);
        poi.setLocalisation(GEO.createPoint(new Coordinate(lng,lat)));
        poi.setAltitudeMetres(altMetres);
        poi.setSource(Source.MANUEL);
        return poi;
    }

}
