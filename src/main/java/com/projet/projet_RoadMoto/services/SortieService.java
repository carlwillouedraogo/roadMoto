package com.projet.projet_RoadMoto.services;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.domain.StatutParticipation;
import com.projet.projet_RoadMoto.dto.SortieRequest;
import com.projet.projet_RoadMoto.dto.SortieResponse;
import com.projet.projet_RoadMoto.dto.UtilisateurResume;
import com.projet.projet_RoadMoto.entity.Participation;
import com.projet.projet_RoadMoto.entity.Sortie;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.exception.MauvaisOrganisateurException;
import com.projet.projet_RoadMoto.exception.MauvaiseSortieException;
import com.projet.projet_RoadMoto.exception.SortieDejaPartieException;
import com.projet.projet_RoadMoto.exception.SortieNotFoundException;
import com.projet.projet_RoadMoto.exception.SuppressionImpossibleException;
import com.projet.projet_RoadMoto.exception.TransitionInvalideException;
import com.projet.projet_RoadMoto.repository.ParticipationRepository;
import com.projet.projet_RoadMoto.repository.SortieRepository;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class SortieService {
    private static final GeometryFactory GEO =
            new GeometryFactory(new PrecisionModel(), 4326);
    private final SortieRepository sortieRepository;
    private final ParticipationService participationService;
    private final ParticipationRepository participationRepository;


    public SortieService(SortieRepository sortieRepository, ParticipationService participationService, ParticipationRepository participationRepository) {
        this.sortieRepository = sortieRepository;
        this.participationService = participationService;
        this.participationRepository = participationRepository;
    }

    /**
     * Méthode permettant à un organisateur de créér une sortie
     * @param request
     * @param organisateur
     * @return SortieResponse
     */
    @Transactional
    public SortieResponse createSortie(SortieRequest request, Utilisateur organisateur){
        Sortie sortie = sortieRepository.save(creeSortie(request, organisateur));
        participationService.inscrireOrganisateur(sortie,organisateur);
        return toResponse(sortie);
    }

    /**
     * Méthode permettant à l'organisateur d'une Sortie de changer le statut en publiée
     * @param id
     * @param organisateur
     * @return SortieResponse
     */

    @Transactional
    public SortieResponse publierSortie(Long id, String organisateur){
        Sortie sortie = sortieRepository.findById(id)
                .orElseThrow(()-> new MauvaiseSortieException("Sortie invalide"));
        if (!sortie.getOrganisateur().getEmail().equals(organisateur)) {
            throw new MauvaisOrganisateurException("Seul l'organisateur peut publier cette sortie");
        }
        if (sortie.getStatut() != Statut.BROUILLON) {
            throw new TransitionInvalideException(sortie.getStatut(), Statut.PUBLIEE);
        }
        sortie.setStatut(Statut.PUBLIEE);
        return toResponse(sortie);
    }

    @Transactional
    public SortieResponse annulerSortie(Long id, String organisateur){
        Sortie sortie = sortieRepository.findById(id)
                .orElseThrow(()-> new MauvaiseSortieException("Sortie invalide"));
        if (!sortie.getOrganisateur().getEmail().equals(organisateur)) {
            throw new MauvaisOrganisateurException("Seul l'organisateur peut annuler cette sortie");
        }
        if (sortie.getStatut() == Statut.BROUILLON){
            throw new TransitionInvalideException(sortie.getStatut(),Statut.ANNULEE);
        }
        if (sortie.getStatut() == Statut.ANNULEE || sortie.getStatut() == Statut.TERMINEE){
            throw new TransitionInvalideException(sortie.getStatut(),Statut.ANNULEE);
        }
        // couvre l'intervalle avant le passage de la tâche planifiée
        if (sortie.getDateDepart().isBefore(OffsetDateTime.now())){
            throw new SortieDejaPartieException("Une sortie déjà partie ne peut pas être annulée");
        }
        sortie.setStatut(Statut.ANNULEE);
        return toResponse(sortie);
    }

    /**
     * Méthode passant en TERMINEE les sorties publiées ou complètes dont la balade est finie
     * @param maintenant
     * @return nombre de sorties terminées
     */

    @Transactional
    public int terminerSortiesPassees(OffsetDateTime maintenant){
        return sortieRepository.terminerSortiesPassees(maintenant);
    }

    /**
     * Méthode permettant d'afficher la liste de toutes les sorties publiées
     * @param niveau
     * @param dateMax
     * @param placesMin
     * @param pageable
     * @return SortieResponse
     */

    @Transactional(readOnly = true)
    public Page<SortieResponse> listeSorties(Niveau niveau, OffsetDateTime dateMax, Integer placesMin, Pageable pageable) {
        Page<Sortie> page = sortieRepository.rechercher(OffsetDateTime.now(), niveau, dateMax, placesMin, pageable);
        return page.map(this::toResponse);
    }

    /**
     * Méthode listant toutes les sorties créées par l'organisateur, brouillons et annulées comprises
     * @param organisateur
     * @param statut filtre optionnel
     * @param pageable
     * @return SortieResponse
     */

    @Transactional(readOnly = true)
    public Page<SortieResponse> historiqueOrganisateur(Utilisateur organisateur, Statut statut, Pageable pageable) {
        return sortieRepository.historiqueOrganisateur(organisateur.getId(), statut, pageable)
                .map(this::toResponse);
    }

    /**
     * Méthode permettant de consulter les détails d'une sortie
     * @param id
     * @param organisateur
     * @return SortieResponse
     */

    @Transactional(readOnly = true)
    public SortieResponse detailSortie(Long id, Utilisateur organisateur){
        Sortie sortie = sortieRepository.findById(id)
                .orElseThrow(()-> new SortieNotFoundException("Sortie non trouvé"));
        boolean estOrganisateur = organisateur != null && sortie.getOrganisateur().getId() == organisateur.getId();

        if (sortie.getStatut() == Statut.BROUILLON && !estOrganisateur){
            throw new SortieNotFoundException("Sortie non trouvé");
        }
        return toResponse(sortie);
    }

    @Transactional
    public void deleteSortie(Long id, Utilisateur organisateur){
        Sortie sortie = sortieRepository.findById(id)
                .orElseThrow(()-> new SortieNotFoundException("Sortie non trouvé"));
        if (sortie.getOrganisateur().getId() != organisateur.getId()){
            throw new MauvaisOrganisateurException("Seul l'organisateur peut supprimer cette sortie");
        }
        // une sortie publiée s'annule, seul un brouillon se supprime
        if (sortie.getStatut() != Statut.BROUILLON) {
            throw new SuppressionImpossibleException("Seul un brouillon peut être supprimé, une sortie publiée doit être annulée");
        }
        sortieRepository.delete(sortie);
    }

    /**
     * Méthode de création d'une sortie
     * @param request
     * @param organisateur
     * @return Sortie
     */

    private Sortie creeSortie(SortieRequest request, Utilisateur organisateur){
        Sortie sortie = new Sortie();
        sortie.setTitre(request.titre());
        sortie.setDescription(request.description());
        sortie.setDateDepart(request.dateDepart());
        sortie.setNomLieuDepart(request.nomLieuDepart());
        sortie.setLieuDepart(GEO.createPoint(new Coordinate(request.longitude(),request.latitude())));
        sortie.setNiveau(request.niveau());
        sortie.setStatut(Statut.BROUILLON);
        sortie.setOrganisateur(organisateur);
        sortie.setPlacesMax(request.placesMax());
        return sortie;
    }

    /**
     * Méthode permettant de convertir une sortie en sortie response
     * @param sortie
     * @return SortieResponse
     */

    private SortieResponse toResponse(Sortie sortie){
        int inscrits = participationRepository.countBySortieIdAndStatut(sortie.getId(), StatutParticipation.INSCRIT);
        Utilisateur u = sortie.getOrganisateur();
        UtilisateurResume utilisateurResume = new UtilisateurResume(u.getId(), u.getPseudo());
        return new SortieResponse(sortie.getId(),sortie.getTitre(),
                sortie.getDescription(),sortie.getDateDepart(),
                sortie.getNomLieuDepart(),sortie.getLieuDepart().getY(),
                sortie.getLieuDepart().getX(),sortie.getNiveau(),
                sortie.getStatut(),sortie.getPlacesMax(),
                sortie.getPlacesMax() - inscrits,
                utilisateurResume,sortie.getCreeLe());
    }
}
