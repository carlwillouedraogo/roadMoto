package com.projet.projet_RoadMoto.controller;

import com.projet.projet_RoadMoto.dto.InscriptionRequest;
import com.projet.projet_RoadMoto.dto.UtilisateurResponse;
import com.projet.projet_RoadMoto.services.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UtilisateurService utilisateurService;

    public AuthController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurResponse inscrire(@Valid @RequestBody InscriptionRequest request){
        return utilisateurService.inscrire(request);
    }
}
