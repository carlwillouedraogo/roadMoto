package com.projet.projet_RoadMoto.controller;

import com.projet.projet_RoadMoto.dto.itinerairedto.CalculItineraireRequest;
import com.projet.projet_RoadMoto.dto.itinerairedto.ItineraireResponse;
import com.projet.projet_RoadMoto.security.UtilisateurPrincipal;
import com.projet.projet_RoadMoto.services.ItineraireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sorties/{sortieId}/itineraire")
@Tag(name = "Itinéraires")
public class ItineraireController {

    private final ItineraireService itineraireService;

    public ItineraireController(ItineraireService itineraireService) {
        this.itineraireService = itineraireService;
    }

    // PUT : l'itinéraire d'une sortie est unique, chaque appel remplace le précédent
    @Operation(
            summary = "Calculer l'itinéraire d'une sortie",
            description = "L'organisateur envoie les étapes ordonnées (départ et arrivée compris), "
                    + "chacune étant un POI existant ou un point libre. Le trajet est calculé via "
                    + "OpenRouteService et remplace l'itinéraire existant."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Itinéraire calculé et enregistré"),
            @ApiResponse(responseCode = "400", description = "Étapes invalides (ex. deux étapes consécutives au même endroit)", content = @Content),
            @ApiResponse(responseCode = "403", description = "L'utilisateur n'est pas l'organisateur", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sortie ou POI introuvable", content = @Content),
            @ApiResponse(responseCode = "409", description = "Sortie annulée ou terminée", content = @Content),
            @ApiResponse(responseCode = "502", description = "Échec du service de calcul d'itinéraire", content = @Content)
    })
    @PutMapping
    public ItineraireResponse calculer(@PathVariable Long sortieId,
                                       @Valid @RequestBody CalculItineraireRequest request,
                                       @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return itineraireService.calculer(sortieId, principal.getUsername(), request);
    }

    @Operation(
            summary = "Consulter l'itinéraire d'une sortie",
            description = "Accessible sans connexion. L'itinéraire d'un brouillon n'est visible que par son organisateur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Itinéraire de la sortie"),
            @ApiResponse(responseCode = "404", description = "Aucun itinéraire, ou sortie en brouillon d'un autre organisateur", content = @Content)
    })
    @GetMapping
    public ItineraireResponse consulter(@PathVariable Long sortieId,
                                        @AuthenticationPrincipal UtilisateurPrincipal principal) {
        return itineraireService.getItineraire(sortieId, principal != null ? principal.getEntite() : null);
    }

    @Operation(summary = "Supprimer l'itinéraire d'une sortie")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Itinéraire supprimé"),
            @ApiResponse(responseCode = "403", description = "L'utilisateur n'est pas l'organisateur", content = @Content),
            @ApiResponse(responseCode = "404", description = "Aucun itinéraire pour cette sortie", content = @Content),
            @ApiResponse(responseCode = "409", description = "Sortie annulée ou terminée", content = @Content)
    })
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long sortieId,
                          @AuthenticationPrincipal UtilisateurPrincipal principal) {
        itineraireService.deleteItineraire(sortieId, principal.getEntite());
    }
}
