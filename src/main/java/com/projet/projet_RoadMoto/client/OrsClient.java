package com.projet.projet_RoadMoto.client;

import com.projet.projet_RoadMoto.dto.itinerairedto.ItineraireCalcule;
import com.projet.projet_RoadMoto.dto.orsdto.OrsFeature;
import com.projet.projet_RoadMoto.dto.orsdto.OrsResponse;
import com.projet.projet_RoadMoto.dto.orsdto.OrsSegment;
import com.projet.projet_RoadMoto.dto.orsdto.OrsSummary;
import com.projet.projet_RoadMoto.dto.etapedto.TronconCalcule;
import com.projet.projet_RoadMoto.exception.EtapesInvalidesException;
import com.projet.projet_RoadMoto.exception.ItineraireIncalculableException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client OpenRouteService : calcule un trajet routier passant par les coordonnées données.
 * ORS n'a pas de profil moto, driving-car est le plus proche.
 */
@Component
public class OrsClient {

    private static final Logger log = LoggerFactory.getLogger(OrsClient.class);
    private static final MediaType GEO_JSON = MediaType.parseMediaType("application/geo+json");

    private static final GeometryFactory GEO =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final RestClient client;

    public OrsClient(RestClient.Builder builder,
                     @Value("${app.ors.url}") String orsUrl,
                     @Value("${app.ors.api-key}") String apiKey) {
        this.client = builder
                .baseUrl(orsUrl)
                .defaultHeader("Authorization", apiKey)
                .build();
    }

    /**
     * Résultat mis en cache par liste de coordonnées : recalculer le même trajet n'appelle pas ORS
     * (quota limité sur l'offre gratuite). Les exceptions ne sont pas mises en cache.
     * @param coordonnees points ordonnés au format [longitude, latitude]
     */
    @Cacheable("itineraires")
    public ItineraireCalcule calculer(List<List<Double>> coordonnees) {
        OrsResponse reponse;
        try {
            // chemin absolu (avec "/") : il s'ajoute à app.ors.url, qui porte l'hôte
            reponse = client.post()
                    .uri("/v2/directions/driving-car/geojson")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(GEO_JSON, MediaType.APPLICATION_JSON)
                    .body(corps(coordonnees))
                    .retrieve()
                    .body(OrsResponse.class);
        } catch (RestClientResponseException e) {
            // ORS a répondu une erreur : on garde son message (ex. point trop loin d'une route, clé refusée)
            log.warn("ORS a répondu {} : {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ItineraireIncalculableException(
                    "Le service de calcul d'itinéraire a refusé la demande (" + e.getStatusCode().value() + ") : "
                            + messageOrs(e), e);
        } catch (RestClientException e) {
            // pas de réponse exploitable : réseau, DNS, timeout, JSON illisible
            log.warn("Appel ORS impossible", e);
            throw new ItineraireIncalculableException("Le service de calcul d'itinéraire est injoignable", e);
        }

        if (reponse == null || reponse.features() == null || reponse.features().isEmpty()) {
            throw new ItineraireIncalculableException("Aucun itinéraire trouvé entre ces étapes");
        }
        OrsFeature feature = reponse.features().getFirst();

        // étapes distinctes mais ramenées par ORS au même point de route : trajet vide, pas de ligne possible
        List<List<Double>> points = feature.geometry().coordinates();
        if (points == null || points.size() < 2 || metres(feature.properties().summary()) < 1) {
            throw new EtapesInvalidesException(
                    "Les étapes sont trop proches : elles correspondent au même point sur la route");
        }

        // un segment ORS = le tronçon entre deux points consécutifs de la requête
        List<OrsSegment> segments = feature.properties().segments();
        List<TronconCalcule> troncons = segments == null ? List.of() : segments.stream()
                .map(s -> new TronconCalcule(arrondi(s.distance()), arrondi(s.duration())))
                .toList();

        return new ItineraireCalcule(
                ligne(points),
                metres(feature.properties().summary()),
                arrondi(feature.properties().summary() == null ? null : feature.properties().summary().duration()),
                troncons);
    }

    // geometry_simplify allège le tracé renvoyé, mais ORS le refuse sur une route de plus d'un segment
    // (donc dès qu'il y a une étape intermédiaire) : on ne l'envoie que pour un trajet départ → arrivée
    private Map<String, Object> corps(List<List<Double>> coordonnees) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("coordinates", coordonnees);
        // pas d'instructions de navigation (steps) : c'est le plus gros de la réponse et on ne s'en sert pas ;
        // segments (distance/durée par tronçon) reste renvoyé
        corps.put("instructions", false);
        if (coordonnees.size() == 2) {
            corps.put("geometry_simplify", true);
        }
        return corps;
    }

    private int metres(OrsSummary summary) {
        return summary == null ? 0 : arrondi(summary.distance());
    }

    // valeur absente = 0 (ORS omet les champs nuls)
    private int arrondi(Double valeur) {
        return valeur == null ? 0 : (int) Math.round(valeur);
    }

    // ORS renvoie {"error":{"code":2010,"message":"..."}} ; on garde le message s'il est lisible
    private String messageOrs(RestClientResponseException e) {
        try {
            Map<?, ?> corps = e.getResponseBodyAs(Map.class);
            if (corps != null && corps.get("error") instanceof Map<?, ?> erreur && erreur.get("message") != null) {
                return String.valueOf(erreur.get("message"));
            }
            if (corps != null && corps.get("error") != null) {
                return String.valueOf(corps.get("error"));
            }
        } catch (RuntimeException ignoree) {
            // corps non JSON : on retombe sur le texte brut
        }
        return e.getResponseBodyAsString();
    }

    private LineString ligne(List<List<Double>> points) {
        Coordinate[] coords = points.stream()
                .map(p -> new Coordinate(p.get(0), p.get(1)))
                .toArray(Coordinate[]::new);
        return GEO.createLineString(coords);
    }
}
