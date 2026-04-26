package com.example.document.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String filePath;
    private String docxFilePath;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @Column(columnDefinition = "LONGTEXT")
    private String extractedDataJson;

    private LocalDateTime uploadTime;
    private LocalDateTime processingTime;
}
