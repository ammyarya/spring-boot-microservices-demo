package com.example.document.controller;

import com.example.document.entity.Document;
import com.example.document.repository.DocumentRepository;
import com.example.document.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService service;

    @Autowired
    private DocumentRepository repository;

    @PostMapping("/upload")
    public ResponseEntity<Document> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Document doc = service.uploadDocument(file);
            return new ResponseEntity<>(doc, HttpStatus.ACCEPTED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam("status") String status) {
        Document doc = repository.findById(id).orElseThrow();
        doc.setStatus(com.example.document.entity.DocumentStatus.valueOf(status));
        repository.save(doc);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/data")
    public ResponseEntity<Void> updateData(@PathVariable Long id, @RequestBody Map<String, String> data) {
        Document doc = repository.findById(id).orElseThrow();
        doc.setExtractedDataJson(data.get("json"));
        doc.setDocxFilePath(data.get("docxPath"));
        doc.setStatus(com.example.document.entity.DocumentStatus.COMPLETED);
        repository.save(doc);
        return ResponseEntity.ok().build();
    }
}
