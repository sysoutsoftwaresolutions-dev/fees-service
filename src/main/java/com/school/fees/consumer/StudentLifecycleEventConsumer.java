package com.school.fees.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.core.common.context.TenantContext;
import com.core.common.event.EventProcessingGuard;
import com.school.fees.event.EventEnvelope;
import com.school.fees.event.InvoiceEventPublisher;
import com.school.fees.event.InvoiceGeneratedEventPayload;
import com.school.fees.model.FeeInvoice;
import com.school.fees.repository.FeeInvoiceRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Consumer for Student lifecycle events. Generates fee invoices upon student registration.
 */
@Component
public class StudentLifecycleEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(StudentLifecycleEventConsumer.class);
    private final FeeInvoiceRepository invoiceRepository;
    private final InvoiceEventPublisher eventPublisher;
    private final EventProcessingGuard eventProcessingGuard;
    private final ObjectMapper objectMapper;

    public StudentLifecycleEventConsumer(FeeInvoiceRepository invoiceRepository,
                                         InvoiceEventPublisher eventPublisher,
                                         EventProcessingGuard eventProcessingGuard,
                                         ObjectMapper objectMapper) {
        this.invoiceRepository = invoiceRepository;
        this.eventPublisher = eventPublisher;
        this.eventProcessingGuard = eventProcessingGuard;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "admission-student-lifecycle", groupId = "fees-service-group")
    public void consumeStudentCreated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String jsonPayload = record.value();
        log.info("[KafkaConsumer] Received event message. Key: {}", record.key());

        try {
            EventEnvelope<MapPayload> envelope = objectMapper.readValue(
                    jsonPayload, new TypeReference<EventEnvelope<MapPayload>>() {}
            );

            String eventId = envelope.getEventId();
            String tenantId = envelope.getTenantId();
            String correlationId = envelope.getCorrelationId();

            if (tenantId == null || tenantId.isBlank()) {
                tenantId = "default";
            }

            // Set TenantContext for dynamic DB routing
            TenantContext.setCurrentTenant(tenantId);

            // Enforce idempotency using the shared EventProcessingGuard
            boolean isNew = eventProcessingGuard.shouldProcess(tenantId, eventId);
            if (!isNew) {
                log.warn("[KafkaConsumer] Duplicate event detected. Skipping execution. EventId: {}", eventId);
                ack.acknowledge();
                return;
            }

            log.info("[KafkaConsumer] Processing StudentCreated event. EventId: {}, TenantId: {}, CorrelationId: {}", 
                    eventId, tenantId, correlationId);

            MapPayload studentData = envelope.getPayload();
            String studentId = studentData.getStudentId();
            
            // Create Tuition Fee Invoice
            String invoiceId = UUID.randomUUID().toString();
            String dueDate = LocalDate.now().plusDays(30).toString();
            double defaultTuitionAmount = 1500.0; // Standard ERP fee

            FeeInvoice invoice = new FeeInvoice(
                    invoiceId,
                    tenantId,
                    studentId,
                    defaultTuitionAmount,
                    dueDate,
                    "UNPAID"
            );

            invoiceRepository.save(invoice);
            log.info("[KafkaConsumer] Tuition Fee Invoice created in DB. InvoiceId: {}, StudentId: {}", invoiceId, studentId);

            // Publish InvoiceGenerated event
            InvoiceGeneratedEventPayload generatedPayload = new InvoiceGeneratedEventPayload(
                    invoiceId, studentId, defaultTuitionAmount, dueDate, "UNPAID"
            );
            eventPublisher.publishInvoiceGenerated(generatedPayload, correlationId, envelope.getOperatorId());

            ack.acknowledge();

        } catch (Exception e) {
            log.error("[KafkaConsumer] Error processing StudentCreated event: {}", e.getMessage(), e);
            throw new RuntimeException("Kafka message processing failed", e);
        } finally {
            TenantContext.clear();
        }
    }

    public static class MapPayload {
        private String studentId;
        private String rollNumber;

        public MapPayload() {}

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }

        public String getRollNumber() { return rollNumber; }
        public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    }
}
