package com.projet.projet_RoadMoto.services;

import com.projet.projet_RoadMoto.domain.Niveau;
import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.domain.StatutParticipation;
import com.projet.projet_RoadMoto.dto.SortieRequest;
import com.projet.projet_RoadMoto.dto.SortieResponse;
import com.projet.projet_RoadMoto.dto.UtilisateurResume;
import com.projet.projet_RoadMoto.entity.Sortie;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.exception.MauvaisOrganisateurException;
import com.projet.projet_RoadMoto.exception.MauvaiseSortieException;
import com.projet.projet_RoadMoto.exception.TransitionInvalideException;
import com.projet.projet_RoadMoto.repository.ParticipationRepository;
import com.projet.projet_RoadMoto.repository.SortieRepository;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class SortieService {
    private static final GeometryFactory GEO =
            new GeometryFactory(new PrecisionModel(), 4326);
    private final SortieRepository sortieRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ParticipationService participationService;
    private final ParticipationRepository participationRepository;


    public SortieService(SortieRepository sortieRepository, UtilisateurRepository utilisateurRepository, ParticipationService participationService, ParticipationRepository participationRepository) {
        this.sortieRepository = sortieRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.participationService = participationService;
        this.participationRepository = participationRepository;
    }

    @Transactional
    public SortieResponse createSortie(SortieRequest request, Utilisateur organisateur){
        Sortie sortie = sortieRepository.save(creeSortie(request, organisateur));
        participationService.inscrireOrganisateur(sortie,organisateur);
        return toResponse(sortie);
    }

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

    @Transactional(readOnly = true)
    public Page<SortieResponse> listeSorties(Niveau niveau, OffsetDateTime dateMax, Integer placesMin, Pageable pageable) {
        Page<Sortie> page = sortieRepository.rechercher(OffsetDateTime.now(), niveau, dateMax, placesMin, pageable);
        return page.map(this::toResponse);
    }

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
