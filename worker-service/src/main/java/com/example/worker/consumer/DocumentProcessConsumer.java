package com.example.worker.consumer;

import com.example.worker.client.DocumentClient;
import com.example.worker.service.DocumentProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class DocumentProcessConsumer {

    @Autowired
    private DocumentClient documentClient;

    @Autowired
    private DocumentProcessor processor;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.notification.queue}")
    private String notificationQueue;

    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void consumeMessage(String documentIdStr) {
        Long documentId = Long.parseLong(documentIdStr);
        log.info("Worker received document processing task for ID: {}", documentId);

        try {
            // 1. Update status to PROCESSING
            documentClient.updateStatus(documentId, "PROCESSING");

            // 2. Get document info (path)
            Map<String, Object> doc = documentClient.getDocumentById(documentId);
            String pdfPath = (String) doc.get("filePath");

            // 3. Process document
            Map<String, String> processedData = processor.process(pdfPath);

            // 4. Update document with extracted data and COMPLETED status
            documentClient.updateData(documentId, processedData);

            // 5. Notify Notification Service
            String notificationMsg = "Document " + documentId + " processed successfully.";
            rabbitTemplate.convertAndSend(notificationQueue, notificationMsg);
            log.info("Sent notification for document ID: {}", documentId);

        } catch (Exception e) {
            log.error("Error in worker processing document {}: {}", documentId, e.getMessage());
            documentClient.updateStatus(documentId, "FAILED");
            rabbitTemplate.convertAndSend(notificationQueue, "Failed to process document " + documentId);
        }
    }
}
