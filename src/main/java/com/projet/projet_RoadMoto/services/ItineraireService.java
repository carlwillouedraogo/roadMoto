package com.projet.projet_RoadMoto.services;

import com.projet.projet_RoadMoto.client.OrsClient;
import com.projet.projet_RoadMoto.domain.Source;
import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.domain.TypeEtape;
import com.projet.projet_RoadMoto.dto.itinerairedto.CalculItineraireRequest;
import com.projet.projet_RoadMoto.dto.etapedto.EtapeRequest;
import com.projet.projet_RoadMoto.dto.etapedto.EtapeResponse;
import com.projet.projet_RoadMoto.dto.itinerairedto.ItineraireCalcule;
import com.projet.projet_RoadMoto.dto.itinerairedto.ItineraireResponse;
import com.projet.projet_RoadMoto.dto.etapedto.TronconCalcule;
import com.projet.projet_RoadMoto.entity.*;
import com.projet.projet_RoadMoto.exception.*;
import com.projet.projet_RoadMoto.repository.ItineraireRepository;
import com.projet.projet_RoadMoto.repository.PoiRepository;
import com.projet.projet_RoadMoto.repository.SortieRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ItineraireService {
    private static final GeometryFactory GEO =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final SortieRepository sortieRepository;
    private final ItineraireRepository itineraireRepository;
    private final PoiRepository poiRepository;
    private final OrsClient orsClient;
    private final TransactionTemplate transactionTemplate;

    public ItineraireService(SortieRepository sortieRepository,
                             ItineraireRepository itineraireRepository,
                             PoiRepository poiRepository,
                             OrsClient orsClient,
                             TransactionTemplate transactionTemplate) {
        this.sortieRepository = sortieRepository;
        this.itineraireRepository = itineraireRepository;
        this.poiRepository = poiRepository;
        this.orsClient = orsClient;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Calcule (ou recalcule) l'itinéraire d'une sortie via OpenRouteService.
     * Volontairement sans @Transactional : l'appel réseau ne doit pas immobiliser une connexion base.
     * @param sortieId
     * @param email email de l'utilisateur connecté, qui doit être l'organisateur
     * @param request étapes ordonnées, départ et arrivée compris
     * @return ItineraireResponse
     */
    public ItineraireResponse calculer(Long sortieId, String email, CalculItineraireRequest request) {

        // 1 — lectures : chaque appel au repository ouvre et ferme sa propre transaction
        Sortie sortie = sortieRepository.findWithOrganisateurById(sortieId)
                .orElseThrow(() -> new SortieIntrouvableException(sortieId));
        verifierOrganisateur(sortie,email);
        verifierModifiable(sortie);
        Map<Long, Poi> pois = chargerPois(request);

        // positions résolues une seule fois : POI s'il y en a un, sinon le point libre
        List<Point> positions = request.etapes().stream()
                .map(e -> e.poiId() != null
                        ? pois.get(e.poiId()).getLocalisation()
                        : GEO.createPoint(new Coordinate(e.longitude(), e.latitude())))
                .toList();

        verifierEtapesDistinctes(positions);

        // 2 — réseau : aucune connexion base immobilisée
        ItineraireCalcule calcule = orsClient.calculer(coordonnees(positions));

        // 3 — écriture : l'ancien itinéraire est remplacé dans une seule transaction,
        // la cascade écrit les étapes avec l'itinéraire
        Itineraire itineraire = construire(sortie, calcule, request, positions, pois);
        Itineraire enregistre = transactionTemplate.execute(status -> {
            itineraireRepository.supprimerParSortieId(sortieId);
            return itineraireRepository.save(itineraire);
        });
        return toResponse(enregistre, sortie);
    }

    /**
     * Affiche l'itineraire d'une sortie.
     * Comme pour le détail d'une sortie, l'itinéraire d'un brouillon n'est visible que par son organisateur :
     * pour les autres, il n'existe pas (404).
     * @param sortieId
     * @param utilisateur utilisateur connecté, null si anonyme
     * @return itineraireResponse
     */
    @Transactional(readOnly = true)
    public ItineraireResponse getItineraire(Long sortieId, Utilisateur utilisateur){
        Itineraire itineraire = itineraireRepository.findWithEtapesById(sortieId)
                .orElseThrow(()-> new ItineraireIntrouvableException(sortieId));
        Sortie sortie = itineraire.getSortie();
        boolean estOrganisateur = utilisateur != null && sortie.getOrganisateur().getId() == utilisateur.getId();
        if (sortie.getStatut() == Statut.BROUILLON && !estOrganisateur) {
            throw new ItineraireIntrouvableException(sortieId);
        }
        return toResponse(itineraire, sortie);
    }

    /**
     * Supprime l'itinéraire d'une sortie, réservé à l'organisateur et refusé si la sortie est annulée ou terminée
     * @param sortieId
     * @param organisateur
     */
    @Transactional
    public void deleteItineraire(Long sortieId, Utilisateur organisateur){
        Itineraire itineraire = itineraireRepository.findById(sortieId)
                .orElseThrow(()-> new ItineraireIntrouvableException(sortieId));
        verifierOrganisateur(itineraire.getSortie(),organisateur.getEmail());
        verifierModifiable(itineraire.getSortie());
        itineraireRepository.delete(itineraire);
    }

    /**
     * Méthode vérifiant que l'utilisateur connecté est bien l'organisateur de la sortie
     * @param sortie
     * @param email
     */
    private void verifierOrganisateur(Sortie sortie, String email) {
        if (!sortie.getOrganisateur().getEmail().equals(email)) {
            throw new MauvaisOrganisateurException("Seul l'organisateur peut modifier l'itinéraire de cette sortie");
        }
    }

    /**
     * Méthode refusant toute modification (calcul ou suppression) sur une sortie annulée ou terminée : l'itinéraire n'a plus d'usage
     * @param sortie
     */
    private void verifierModifiable(Sortie sortie) {
        if (sortie.getStatut() == Statut.ANNULEE || sortie.getStatut() == Statut.TERMINEE) {
            throw new ItineraireNonModifiableException(
                    "L'itinéraire d'une sortie " + sortie.getStatut().name().toLowerCase() + " ne peut plus être modifié");
        }
    }

    /**
     * Méthode refusant deux étapes consécutives au même endroit : ORS renverrait un tronçon vide.
     * Une boucle (départ = arrivée) reste possible tant qu'il y a des étapes entre les deux.
     * @param positions
     */
    private void verifierEtapesDistinctes(List<Point> positions) {
        for (int i = 1; i < positions.size(); i++) {
            if (positions.get(i).getCoordinate().equals2D(positions.get(i - 1).getCoordinate())) {
                throw new EtapesInvalidesException(
                        "Les étapes " + (i - 1) + " et " + i + " sont au même endroit");
            }
        }
    }

    /**
     * Méthode chargeant en une seule requête tous les POI référencés par les étapes
     * @param request
     * @return POI indexés par id
     */
    private Map<Long, Poi> chargerPois(CalculItineraireRequest request) {
        Set<Long> ids = request.etapes().stream()
                .map(EtapeRequest::poiId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Poi> pois = poiRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Poi::getId, Function.identity()));
        ids.stream()
                .filter(id -> !pois.containsKey(id))
                .findFirst()
                .ifPresent(id -> { throw new PoiIntrouvableException(id); });
        return pois;
    }

    /**
     * Méthode convertissant les étapes en coordonnées [longitude, latitude] attendues par ORS
     * @param positions
     * @return coordonnées ordonnées
     */
    private List<List<Double>> coordonnees(List<Point> positions) {
        return positions.stream()
                .map(p -> List.of(p.getX(), p.getY()))
                .toList();
    }

    /**
     * Méthode construisant l'entité itinéraire et ses étapes à partir du calcul ORS
     * @param sortie
     * @param calcule
     * @param request
     * @param positions positions des étapes, dans le même ordre que la requête
     * @param pois
     * @return Itineraire
     */
    private Itineraire construire(Sortie sortie, ItineraireCalcule calcule, CalculItineraireRequest request,
                                  List<Point> positions, Map<Long, Poi> pois) {
        List<EtapeRequest> demandes = request.etapes();
        List<TronconCalcule> troncons = calcule.troncons();
        // ORS renvoie un tronçon par paire d'étapes consécutives
        if (troncons.size() != demandes.size() - 1) {
            throw new ItineraireIncalculableException("Réponse du service de calcul incohérente avec les étapes demandées");
        }

        Itineraire itineraire = new Itineraire();
        itineraire.setSortie(sortie);
        itineraire.setTrace(calcule.trace());
        itineraire.setDistanceM(calcule.distanceM());
        itineraire.setDureeS(calcule.dureeS());
        itineraire.setSource(Source.OPENROUTESERVICE);

        for (int i = 0; i < demandes.size(); i++) {
            EtapeRequest demande = demandes.get(i);
            Etape etape = new Etape();
            etape.setPosition(positions.get(i));
            etape.setPoi(demande.poiId() == null ? null : pois.get(demande.poiId()));
            etape.setTypeEtape(i == 0 ? TypeEtape.DEPART : i == demandes.size() - 1 ? TypeEtape.ARRIVEE : TypeEtape.INTERMEDIAIRE);
            etape.setDureeArretMin(demande.dureeArretMin());
            // la première étape n'a pas de précédente
            if (i > 0) {
                TronconCalcule troncon = troncons.get(i - 1);
                etape.setDistanceDepuisPrecedenteM(troncon.distanceM());
                etape.setDureeDepuisPrecedenteS(troncon.dureeS());
            }
            itineraire.ajouterEtape(etape);
        }
        return itineraire;
    }

    /**
     * Méthode convertissant un itinéraire en ItineraireResponse,
     * avec l'heure de passage estimée à chaque étape (trajets + arrêts cumulés)
     * @param itineraire
     * @param sortie
     * @return ItineraireResponse
     */
    private ItineraireResponse toResponse(Itineraire itineraire, Sortie sortie) {
        OffsetDateTime heure = sortie.getDateDepart();
        List<EtapeResponse> etapes = new ArrayList<>();
        for (Etape e : itineraire.getEtapes()) {
            if (e.getDureeDepuisPrecedenteS() != null) {
                heure = heure.plusSeconds(e.getDureeDepuisPrecedenteS());
            }
            Poi poi = e.getPoi();
            etapes.add(new EtapeResponse(
                    e.getOrdre(),
                    e.getTypeEtape(),
                    e.getPosition().getX(),
                    e.getPosition().getY(),
                    poi == null ? null : poi.getId(),
                    poi == null ? null : poi.getNom(),
                    e.getDistanceDepuisPrecedenteM() == null ? null : arrondiKm(e.getDistanceDepuisPrecedenteM()),
                    e.getDureeDepuisPrecedenteS() == null ? null : (int) Math.round(e.getDureeDepuisPrecedenteS() / 60.0),
                    e.getDureeArretMin(),
                    heure));
            if (e.getDureeArretMin() != null) {
                heure = heure.plusMinutes(e.getDureeArretMin());
            }
        }

        List<List<Double>> trace = Arrays.stream(itineraire.getTrace().getCoordinates())
                .map(c -> List.of(c.getX(), c.getY()))
                .toList();

        return new ItineraireResponse(
                sortie.getId(),
                arrondiKm(itineraire.getDistanceM()),
                (int) Math.round(itineraire.getDureeS() / 60.0),
                trace,
                etapes,
                itineraire.getSource(),
                itineraire.getCalculeLe());
    }


    // distance en km arrondie à 0,1 km
    private double arrondiKm(int metres) {
        return Math.round(metres / 100.0) / 10.0;
    }
}
