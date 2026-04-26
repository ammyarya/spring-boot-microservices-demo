package com.example.document.service;

import com.example.document.entity.Document;
import com.example.document.entity.DocumentStatus;
import com.example.document.repository.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class DocumentService {

    @Autowired
    private DocumentRepository repository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Document uploadDocument(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        Document doc = Document.builder()
                .fileName(file.getOriginalFilename())
                .filePath(filePath.toString())
                .status(DocumentStatus.UPLOADED)
                .uploadTime(LocalDateTime.now())
                .build();

        doc = repository.save(doc);

        // Publish event to RabbitMQ
        rabbitTemplate.convertAndSend(exchange, routingKey, doc.getId().toString());
        log.info("Published event for document ID: {}", doc.getId());

        return doc;
    }

    public void processDocument(Long documentId) {
        Document doc = repository.findById(documentId).orElseThrow();
        try {
            doc.setStatus(DocumentStatus.PROCESSING);
            doc.setProcessingTime(LocalDateTime.now());
            repository.save(doc);

            // 1. Extract Text from PDF
            String extractedText = extractTextFromPdf(doc.getFilePath());

            // 2. Convert to DOCX (Simple implementation)
            String docxPath = convertTextToDocx(extractedText, doc.getFilePath());
            doc.setDocxFilePath(docxPath);

            // 3. Convert Extracted Data to JSON
            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("content", extractedText);
            dataMap.put("metadata", "Extracted on " + LocalDateTime.now());
            String json = objectMapper.writeValueAsString(dataMap);
            doc.setExtractedDataJson(json);

            doc.setStatus(DocumentStatus.COMPLETED);
            repository.save(doc);
            log.info("Document processing completed for ID: {}", documentId);

        } catch (Exception e) {
            log.error("Error processing document: {}", e.getMessage());
            doc.setStatus(DocumentStatus.FAILED);
            repository.save(doc);
        }
    }

    private String extractTextFromPdf(String pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String convertTextToDocx(String text, String pdfPath) throws IOException {
        String docxPath = pdfPath.replace(".pdf", ".docx");
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(docxPath)) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(text);
            document.write(out);
        }
        return docxPath;
    }
}
