package com.projet.projet_RoadMoto.services;

import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.dto.SortieRequest;
import com.projet.projet_RoadMoto.dto.SortieResponse;
import com.projet.projet_RoadMoto.dto.UtilisateurResume;
import com.projet.projet_RoadMoto.entity.Sortie;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.repository.SortieRepository;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SortieService {
    private static final GeometryFactory GEO =
            new GeometryFactory(new PrecisionModel(), 4326);
    private final SortieRepository sortieRepository;
    private final UtilisateurRepository utilisateurRepository;


    public SortieService(SortieRepository sortieRepository, UtilisateurRepository utilisateurRepository) {
        this.sortieRepository = sortieRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional
    public SortieResponse createSortie(SortieRequest request, String organisateur){
        Utilisateur organisateur1 = utilisateurRepository.findByEmail(organisateur);
        if(!utilisateurRepository.existsByEmail(organisateur1.getEmail())){
            throw new UsernameNotFoundException("Utilisateur non trouvé");
        }
        Sortie sortie = creeSortie(request);
        sortie.setOrganisateur(organisateur1);
        return toResponse(sortieRepository.save(sortie));

    }

    private Sortie creeSortie(SortieRequest request){
        Sortie sortie = new Sortie();
        sortie.setTitre(request.titre());
        sortie.setDescription(request.description());
        sortie.setDateDepart(request.dateDepart());
        sortie.setNomLieuDepart(request.nomLieuDepart());
        sortie.setLieuDepart(GEO.createPoint(new Coordinate(request.longitude(),request.latitude())));
        sortie.setNiveau(request.niveau());
        sortie.setStatut(Statut.BROUILLON);
        sortie.setPlacesMax(request.placesMax());
        return sortie;
    }

    private SortieResponse toResponse(Sortie sortie){
        Utilisateur u = sortie.getOrganisateur();
        UtilisateurResume utilisateurResume = new UtilisateurResume(u.getId(), u.getPseudo());
        return new SortieResponse(sortie.getId(),sortie.getTitre(),
                sortie.getDescription(),sortie.getDateDepart(),
                sortie.getNomLieuDepart(),sortie.getLieuDepart().getY(),
                sortie.getLieuDepart().getX(),sortie.getNiveau(),
                sortie.getStatut(),sortie.getPlacesMax(),
                sortie.getPlacesMax(),
                utilisateurResume,sortie.getCreeLe());
    }
}
