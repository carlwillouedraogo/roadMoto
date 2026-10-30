package com.projet.projet_RoadMoto.services;

import com.projet.projet_RoadMoto.domain.Statut;
import com.projet.projet_RoadMoto.domain.StatutParticipation;
import com.projet.projet_RoadMoto.dto.HistoriqueParticipationResponse;
import com.projet.projet_RoadMoto.dto.ParticipationResponse;
import com.projet.projet_RoadMoto.dto.SortieResume;
import com.projet.projet_RoadMoto.dto.UtilisateurResume;
import com.projet.projet_RoadMoto.entity.Participation;
import com.projet.projet_RoadMoto.entity.Sortie;
import com.projet.projet_RoadMoto.entity.Utilisateur;
import com.projet.projet_RoadMoto.exception.*;
import com.projet.projet_RoadMoto.repository.ParticipationRepository;
import com.projet.projet_RoadMoto.repository.SortieRepository;
import com.projet.projet_RoadMoto.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;


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
    public ParticipationResponse sInscrire(Long sortieId, Utilisateur participant){
        Sortie sortie = sortieRepository.findByIdPourInscription(sortieId)
                .orElseThrow(() -> new SortieNotFoundException("Cette sortie n'existe pas"));

        if (sortie.getStatut() == Statut.COMPLETE){
            throw new SortieCompleteException(sortieId);
        }
        if (sortie.getStatut() != Statut.PUBLIEE){
            throw new InscriptionImpossibleException("Cette sortie n'accepte pas d'inscription");
        }
        if (sortie.getDateDepart().isBefore(OffsetDateTime.now())){
            throw new InscriptionImpossibleException("Cette sortie est déjà passé");
        }
        Optional<Participation> existante = participationRepository.findBySortieIdAndUtilisateurId(sortieId, participant.getId());

        if(existante.filter(p -> p.getStatut() == StatutParticipation.INSCRIT).isPresent()){
            throw new DejaInscritException("Vous êtes déjà inscrit à cette sortie");
        }
        int inscrits = participationRepository.countBySortieIdAndStatut(sortieId, StatutParticipation.INSCRIT);

        if (inscrits >= sortie.getPlacesMax()){
            throw new SortieCompleteException(sortieId);
        }
        // un participant qui s'était désisté réutilise sa ligne (contrainte unique sortie/utilisateur)
        Participation participation = existante
                .map(this::reactiver)
                .orElseGet(() -> enregistrer(sortie, participant,StatutParticipation.INSCRIT));

        if (inscrits + 1 == sortie.getPlacesMax()) {
            sortie.setStatut(Statut.COMPLETE);
        }

        return creerParticipationResponse(participation, sortie.getPlacesMax() - (inscrits + 1));
    }

    @Transactional
    public Participation inscrireOrganisateur(Sortie sortie, Utilisateur organisateur) {
        return enregistrer(sortie, organisateur,StatutParticipation.INSCRIT);
    }

    @Transactional
    public ParticipationResponse desInscription(Long id, Utilisateur participant){
        Sortie sortie = sortieRepository.findByIdPourInscription(id)
                .orElseThrow(()->new SortieNotFoundException("Cette sortie n'existe pas"));
        Participation participation = participationRepository.findBySortieIdAndUtilisateurId(id, participant.getId())
                .filter(p -> p.getStatut() == StatutParticipation.INSCRIT)
                .orElseThrow(()->new NonInscritException("Vous n'êtes pas inscrit à cette sortie"));

        if (sortie.getOrganisateur().getId() == participant.getId()){
            throw new InscriptionImpossibleException("L'organisateur ne peut pas se désincrire de sa sortie");
        }
        if (sortie.getDateDepart().isBefore(OffsetDateTime.now())){
            throw new SortieDejaPartieException("On ne peut pas se désinscrire d'une sortie déjà partie");
        }

        participation.setStatut(StatutParticipation.DESIST);

        // une place se libère : une sortie complète redevient ouverte
        if(sortie.getStatut() == Statut.COMPLETE){
            sortie.setStatut(Statut.PUBLIEE);
        }

        // le count déclenche un flush automatique, il tient donc compte du désistement
        int inscrits = participationRepository.countBySortieIdAndStatut(id,StatutParticipation.INSCRIT);

        return creerParticipationResponse(participation, sortie.getPlacesMax()-inscrits);
    }

    /**
     * Méthode listant les sorties auxquelles l'utilisateur s'est inscrit, désistements compris
     * @param participant
     * @param statut filtre optionnel
     * @param pageable
     * @return HistoriqueParticipationResponse
     */

    @Transactional(readOnly = true)
    public Page<HistoriqueParticipationResponse> historique(Utilisateur participant, StatutParticipation statut, Pageable pageable){
        return participationRepository.historique(participant.getId(), statut, pageable)
                .map(this::toHistorique);
    }

    private Participation reactiver(Participation participation) {
        participation.setStatut(StatutParticipation.INSCRIT);
        participation.setInscritLe(OffsetDateTime.now());
        return participation;
    }

    private Participation enregistrer(Sortie sortie, Utilisateur utilisateur, StatutParticipation statutParticipation) {
        Participation p = new Participation();
        p.setSortie(sortie);
        p.setUtilisateur(utilisateur);
        p.setStatut(statutParticipation);
        return participationRepository.save(p);
    }

    private HistoriqueParticipationResponse toHistorique(Participation participation){
        Sortie s = participation.getSortie();
        Utilisateur o = s.getOrganisateur();
        SortieResume sortieResume = new SortieResume(s.getId(), s.getTitre(),
                s.getDescription(), s.getDateDepart(),
                s.getNomLieuDepart(), s.getLieuDepart().getY(),
                s.getLieuDepart().getX(), s.getNiveau(),
                s.getStatut(), s.getPlacesMax(),
                new UtilisateurResume(o.getId(), o.getPseudo()), s.getCreeLe());
        return new HistoriqueParticipationResponse(participation.getId(), participation.getStatut(),
                participation.getInscritLe(), sortieResume);
    }

    private ParticipationResponse creerParticipationResponse(Participation participation, int placesRestantes){
        Utilisateur u = participation.getUtilisateur();
        UtilisateurResume utilisateurResume = new UtilisateurResume(u.getId(), u.getPseudo());
        ParticipationResponse participationResponse = new ParticipationResponse(participation.getId(),participation.getSortie().getId(),
                utilisateurResume,participation.getStatut(),
                participation.getInscritLe(), placesRestantes);
        return participationResponse;
    }
}
