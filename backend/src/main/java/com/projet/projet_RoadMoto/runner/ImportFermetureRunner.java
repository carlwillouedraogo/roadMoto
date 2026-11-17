package com.projet.projet_RoadMoto.runner;

import com.projet.projet_RoadMoto.services.FermetureImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("import-fermetures")
public class ImportFermetureRunner implements CommandLineRunner {
    private final FermetureImportService service;

    public ImportFermetureRunner(FermetureImportService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) throws Exception {
        service.appliquerFermetures();
    }
}
