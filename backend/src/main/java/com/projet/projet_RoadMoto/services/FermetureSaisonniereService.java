package com.projet.projet_RoadMoto.services;

import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.dto.AvertissementResponse;
import com.projet.projet_RoadMoto.entity.FermetureSaisonniere;
import com.projet.projet_RoadMoto.entity.Poi;
import com.projet.projet_RoadMoto.entity.Sortie;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.exception.ItineraireIntrouvableException;
import com.projet.projet_RoadMoto.exception.SortieIntrouvableException;
import com.projet.projet_RoadMoto.repository.FermetureSaisonniereRepository;
import com.projet.projet_RoadMoto.repository.ItineraireRepository;
import com.projet.projet_RoadMoto.repository.SortieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
public class FermetureSaisonniereService {
    private final FermetureSaisonniereRepository fermetureSaisonniereRepository;
    private final SortieRepository sortieRepository;
    private final ItineraireRepository itineraireRepository;
    private final int toleranceMetres;

    public FermetureSaisonniereService(FermetureSaisonniereRepository fermetureSaisonniereRepository,
                                       SortieRepository sortieRepository,
                                       ItineraireRepository itineraireRepository,
                                       @Value("${app.fermetures.tolerance-metres:500}") int toleranceMetres) {
        this.fermetureSaisonniereRepository = fermetureSaisonniereRepository;
        this.sortieRepository = sortieRepository;
        this.itineraireRepository = itineraireRepository;
        this.toleranceMetres = toleranceMetres;
    }

    @Transactional(readOnly = true)
    public List<AvertissementResponse> avertissements(Long sortieId, OffsetDateTime dateDepart){
        return avertissementsPourMois(sortieId, dateDepart.getMonthValue());
    }

    /**
     * Avertissements de fermeture des cols traversés par l'itinéraire d'une sortie.
     * Permet de simuler une autre date que la date de départ prévue (ex. pour choisir quand partir).
     * Comme pour l'itinéraire, un brouillon n'est visible que par son organisateur (404 sinon).
     * @param sortieId
     * @param date date à tester, null pour utiliser la date de départ de la sortie
     * @param utilisateur utilisateur connecté, null si anonyme
     */
    @Transactional(readOnly = true)
    public List<AvertissementResponse> avertissementsPourSortie(Long sortieId, LocalDate date, Utilisateur utilisateur) {
        Sortie sortie = sortieRepository.findWithOrganisateurById(sortieId)
                .orElseThrow(() -> new SortieIntrouvableException(sortieId));
        boolean estOrganisateur = utilisateur != null && sortie.getOrganisateur().getId() == utilisateur.getId();
        if (sortie.getStatut() == Statut.BROUILLON && !estOrganisateur) {
            throw new SortieIntrouvableException(sortieId);
        }
        if (!itineraireRepository.existsById(sortieId)) {
            throw new ItineraireIntrouvableException(sortieId);
        }
        int mois = date != null ? date.getMonthValue() : sortie.getDateDepart().getMonthValue();
        return avertissementsPourMois(sortieId, mois);
    }

    private List<AvertissementResponse> avertissementsPourMois(Long sortieId, int mois) {
        return fermetureSaisonniereRepository.colsTraverses(sortieId,toleranceMetres).stream()
                .filter(f -> !f.estOuvertEn(mois))
                .map(f -> versAvertissement(f, mois))
                .toList();
    }

    private AvertissementResponse versAvertissement(FermetureSaisonniere fermeture, int mois) {
        Poi col = fermeture.getPoi();
        String altitude = col.getAltitudeMetres() == null
                ? ""
                : " (%d m)".formatted(col.getAltitudeMetres());

        String message = "%s%s : généralement fermé en %s, ouvert de %s à %s.".formatted(
                col.getNom(),
                altitude,
                nomDuMois(mois),
                nomDuMois(fermeture.getMoisOuvertureDebut()),
                nomDuMois(fermeture.getMoisOuvertureFin()));

        return new AvertissementResponse(
                fermeture.getPoiId(),
                col.getNom(),
                col.getAltitudeMetres(),
                fermeture.getMoisOuvertureDebut(),
                fermeture.getMoisOuvertureFin(),
                message);
    }

    private static String nomDuMois(int mois) {
        return Month.of(mois).getDisplayName(TextStyle.FULL, Locale.FRENCH);
    }
}
