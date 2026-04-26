package com.example.worker.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "document-service")
public interface DocumentClient {

    @GetMapping("/api/documents/{id}")
    Map<String, Object> getDocumentById(@PathVariable("id") Long id);

    @PutMapping("/api/documents/{id}/status")
    void updateStatus(@PathVariable("id") Long id, @RequestParam("status") String status);

    @PutMapping("/api/documents/{id}/data")
    void updateData(@PathVariable("id") Long id, @RequestBody Map<String, String> data);
}
