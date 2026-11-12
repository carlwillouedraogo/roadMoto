package com.projet.projet_RoadMoto.controller;

import com.projet.projet_RoadMoto.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GestionnaireErreurs {

    @ExceptionHandler(SortieNotFoundException.class)
    public ProblemDetail sortieIntrouvable(SortieNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(DejaInscritException.class)
    public ProblemDetail dejaInscrit(DejaInscritException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(NonInscritException.class)
    public ProblemDetail nonInscrit(NonInscritException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(MauvaisOrganisateurException.class)
    public ProblemDetail accesRefuse(MauvaisOrganisateurException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(InscriptionImpossibleException.class)
    public ProblemDetail inscriptionImpossible(InscriptionImpossibleException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(SuppressionImpossibleException.class)
    public ProblemDetail suppressionImpossible(SuppressionImpossibleException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(SortieDejaPartieException.class)
    public ProblemDetail sortieDejaPartie(SortieDejaPartieException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(SortieCompleteException.class)
    public ProblemDetail sortieComplete(SortieCompleteException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(TransitionInvalideException.class)
    public ProblemDetail transitionInvalide(TransitionInvalideException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(MauvaiseSortieException.class)
    public ProblemDetail mauvaiseSortie(MauvaiseSortieException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(EmailDejaUtiliseException.class)
    public ProblemDetail emailDejaUtilise(EmailDejaUtiliseException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(SortieIntrouvableException.class)
    public ProblemDetail sortieIntrouvableParId(SortieIntrouvableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Sortie " + e.getMessage() + " introuvable");
    }

    @ExceptionHandler(PoiIntrouvableException.class)
    public ProblemDetail poiIntrouvable(PoiIntrouvableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ItineraireIncalculableException.class)
    public ProblemDetail itineraireIncalculable(ItineraireIncalculableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(ItineraireNonModifiableException.class)
    public ProblemDetail itineraireNonModifiable(ItineraireNonModifiableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ItineraireIntrouvableException.class)
    public ProblemDetail itineraireIntrouvable(ItineraireIntrouvableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(EtapesInvalidesException.class)
    public ProblemDetail etapesInvalides(EtapesInvalidesException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
