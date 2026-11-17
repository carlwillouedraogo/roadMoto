package com.projet.projet_RoadMoto.services;



import com.projet.projet_RoadMoto.domain.ResultatImport;
import com.projet.projet_RoadMoto.dto.poidto.FermetureReference;
import com.projet.projet_RoadMoto.entity.FermetureSaisonniere;
import com.projet.projet_RoadMoto.entity.Poi;
import com.projet.projet_RoadMoto.repository.FermetureSaisonniereRepository;
import com.projet.projet_RoadMoto.repository.PoiRepository;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
public class FermetureImportService {
    private final PoiRepository poiRepository;
    private final FermetureSaisonniereRepository fermetureRepository;
    private final ObjectMapper mapper;
    private static final Logger log = LoggerFactory.getLogger(FermetureImportService.class);

    public FermetureImportService(PoiRepository poiRepository, FermetureSaisonniereRepository fermetureRepository, ObjectMapper mapper) {
        this.poiRepository = poiRepository;
        this.fermetureRepository = fermetureRepository;
        this.mapper = mapper;
    }

    @Transactional
    public ResultatImport appliquerFermetures() {
        List<FermetureReference> references = lireFichier();
        int appliquees = 0, introuvables = 0;

        for (FermetureReference ref : references) {
            Optional<Poi> poi = poiRepository.findByOsmId(ref.osmId());
            if (poi.isEmpty()) {
                log.warn("POI introuvable pour osmId {} ({})", ref.osmId(), ref.nom());
                introuvables++;
                continue;
            }
            // upsert de la FermetureSaisonniere
            Poi p = poi.get();

            FermetureSaisonniere fermeture = fermetureRepository.findById(p.getId())
                    .orElseGet(() -> {
                        FermetureSaisonniere nouvelle = new FermetureSaisonniere();
                        nouvelle.setPoi(p);
                        return nouvelle;
                    });

            // champ absent du JSON => null => considéré comme fermé l'hiver
            boolean ouvertToutAnnee = Boolean.TRUE.equals(ref.ouvertToutAnnee());
            fermeture.setOuvertTouteAnnee(ouvertToutAnnee);
            if (ouvertToutAnnee) {
                fermeture.setMoisOuvertureDebut(null);
                fermeture.setMoisOuvertureFin(null);
            } else {
                fermeture.setMoisOuvertureDebut(ref.ouvertureDebut());
                fermeture.setMoisOuvertureFin(ref.ouvertureFin());
            }

            fermetureRepository.save(fermeture);
            appliquees++;
        }
        return new ResultatImport(appliquees, 0, introuvables);
    }

    private List<FermetureReference> lireFichier() {
        try (InputStream in = new ClassPathResource("fermetures-cols.json").getInputStream()) {
            return mapper.readValue(in, new TypeReference<List<FermetureReference>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Lecture de fermetures-cols.json impossible", e);
        }
    }
}
