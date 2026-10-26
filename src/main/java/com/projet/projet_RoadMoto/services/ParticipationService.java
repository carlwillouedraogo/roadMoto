package com.projet.projet_RoadMoto.services;

import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.domain.StatutParticipation;
import com.projet.projet_RoadMoto.dto.ParticipationResponse;
import com.projet.projet_RoadMoto.dto.UtilisateurResume;
import com.projet.projet_RoadMoto.entity.Participation;
import com.projet.projet_RoadMoto.entity.Sortie;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.exception.*;
import com.projet.projet_RoadMoto.repository.ParticipationRepository;
import com.projet.projet_RoadMoto.repository.SortieRepository;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;


@Service
public class ParticipationService {
    private final ParticipationRepository participationRepository;
    private final SortieRepository sortieRepository;
    private final UtilisateurRepository utilisateurRepository;

    public ParticipationService(ParticipationRepository participationRepository, SortieRepository sortieRepository, UtilisateurRepository utilisateurRepository) {
        this.participationRepository = participationRepository;
        this.sortieRepository = sortieRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional
    public ParticipationResponse sInscrire(Long sortieId, String participant){
        Utilisateur utilisateur = utilisateurRepository.findByEmail(participant)
                .orElseThrow(() -> new UtilisateurIntrouvableException(participant));
        Sortie sortie = sortieRepository.findByIdPourInscription(sortieId)
                .orElseThrow(() -> new SortieNotFoundException("Cette sortie n'existe pas"));

        if (sortie.getStatut() != Statut.PUBLIEE){
            throw new InscriptionImpossibleException("Cette sortie n'accepte pas d'inscription");
        }
        if (sortie.getDateDepart().isBefore(OffsetDateTime.now())){
            throw new InscriptionImpossibleException("Cette sortie est déjà passé");
        }
        if(participationRepository.existsBySortieIdAndUtilisateurId(sortieId, utilisateur.getId())){
            throw new DejaInscritException("Vous êtes déjà inscrit à cette sortie");
        }
        int inscrits = participationRepository.countBySortieIdAndStatut(sortieId, StatutParticipation.INSCRIT);

        if (inscrits >= sortie.getPlacesMax()){
            throw new SortieCompleteException(sortieId);
        }
        Participation participation = enregistrer(sortie, utilisateur);

        if (inscrits + 1 == sortie.getPlacesMax()) {
            sortie.setStatut(Statut.COMPLETE);
        }

        return creerParticipationResponse(participation, sortie.getPlacesMax() - (inscrits + 1));

    }

    @Transactional
    public Participation inscrireOrganisateur(Sortie sortie, Utilisateur organisateur) {
        return enregistrer(sortie, organisateur);
    }

    private Participation enregistrer(Sortie sortie, Utilisateur utilisateur) {
        Participation p = new Participation();
        p.setSortie(sortie);
        p.setUtilisateur(utilisateur);
        p.setStatut(StatutParticipation.INSCRIT);
        return participationRepository.save(p);
    }

    private ParticipationResponse creerParticipationResponse(Participation participation, int placesRestantes){
        Utilisateur u = participation.getUtilisateur();
        UtilisateurResume utilisateurResume = new UtilisateurResume(u.getId(), u.getPseudo());
        placesRestantes = participationRepository.countBySortieIdAndStatut(participation.getSortie().getId(),StatutParticipation.INSCRIT);
        ParticipationResponse participationResponse = new ParticipationResponse(participation.getId(),participation.getSortie().getId(),
                utilisateurResume,participation.getStatut(),
                participation.getInscritLe(),participation.getSortie().getPlacesMax() - placesRestantes) ;
        return participationResponse;
    }

    private Participation creerParticipation(Utilisateur utilisateur, Sortie sortie){
        Participation participation = new Participation();
        participation.setSortie(sortie);
        participation.setUtilisateur(utilisateur);
        participation.setStatut(StatutParticipation.INSCRIT);
        return participation;
    }

}
