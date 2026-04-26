package com.example.worker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class DocumentProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, String> process(String pdfPath) throws IOException {
        log.info("Starting processing for file: {}", pdfPath);
        
        // 1. Extract Text
        String text = extractText(pdfPath);
        
        // 2. Convert to DOCX
        String docxPath = pdfPath.replace(".pdf", ".docx");
        saveToDocx(text, docxPath);
        
        // 3. Create JSON Data
        Map<String, String> result = new HashMap<>();
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("content", text);
        dataMap.put("processedAt", LocalDateTime.now().toString());
        
        result.put("json", objectMapper.writeValueAsString(dataMap));
        result.put("docxPath", docxPath);
        
        return result;
    }

    private String extractText(String pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private void saveToDocx(String text, String docxPath) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(docxPath)) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(text);
            document.write(out);
        }
    }
}
