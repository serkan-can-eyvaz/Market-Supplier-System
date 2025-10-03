package com.example.marketsupplier.controller;

import com.example.marketsupplier.agent.nlp.IntentConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final IntentConfigLoader loader;

    public AdminController(IntentConfigLoader loader) {
        this.loader = loader;
    }

    @PostMapping("/reload-intents")
    public ResponseEntity<String> reloadIntents() {
        loader.reload();
        log.info("Intents configuration reloaded via admin endpoint");
        return ResponseEntity.ok("Intents reloaded");
    }
}


