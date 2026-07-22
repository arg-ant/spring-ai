package com.multimodel.llm.controller;

import com.multimodel.llm.service.VectorStoreDataService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing endpoints to load and remove sample data in the vector store on
 * demand: a fixed set of general-knowledge sentences, and the HR policies PDF document.
 */
@RestController
@RequestMapping("/api/rag")
public class VectorStoreDataController {

    private final VectorStoreDataService randomDataService;

    /**
     * Creates a new controller backed by the given service.
     *
     * @param service service used to load and remove documents in the vector store
     */
    public VectorStoreDataController(VectorStoreDataService service) {
        this.randomDataService = service;
    }

    /**
     * Loads the sample sentences into the vector store.
     *
     * @return a confirmation message
     */
    @PostMapping("/random/load")
    public String loadRandom() {
        randomDataService.loadRandom();
        return "Random data loaded";
    }

    /**
     * Removes the previously loaded sample sentences from the vector store.
     *
     * @return a confirmation message
     */
    @DeleteMapping("/random/remove")
    public String removeRandom() {
        randomDataService.removeRandom();
        return "Random data removed";
    }

    /**
     * Loads the HR policies PDF document into the vector store.
     *
     * @return a confirmation message
     */
    @PostMapping("/document/load")
    public String loadPDF() {
        randomDataService.loadPDF();
        return "Document data loaded";
    }

    /**
     * Removes the previously loaded HR policies PDF document from the vector store.
     *
     * @return a confirmation message
     */
    @DeleteMapping("/document/remove")
    public String removePDF() {
        randomDataService.removePDF();
        return "Document data removed";
    }

}
