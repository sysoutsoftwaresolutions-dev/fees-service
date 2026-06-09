package com.school.fees.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.core.common.context.TenantContext;
import com.core.common.event.OutboxPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Component to publish fee payment lifecycle events (InvoiceGenerated, FeePaid) via the outbox database.
 */
@Component
public class InvoiceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InvoiceEventPublisher.class);
    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;

    public InvoiceEventPublisher(OutboxPublisher outboxPublisher, ObjectMapper objectMapper) {
        this.outboxPublisher = outboxPublisher;
        this.objectMapper = objectMapper;
    }

    public void publishInvoiceGenerated(InvoiceGeneratedEventPayload payload, String correlationId, String operatorId) {
        publishEvent("InvoiceGenerated", payload, correlationId, operatorId);
    }

    public void publishFeePaid(FeePaidEventPayload payload, String correlationId, String operatorId) {
        publishEvent("FeePaid", payload, correlationId, operatorId);
    }

    private <T> void publishEvent(String eventType, T payload, String correlationId, String operatorId) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            tenantId = "default";
        }

        EventEnvelope<T> envelope = new EventEnvelope<>(
                correlationId != null ? correlationId : UUID.randomUUID().toString(),
                eventType,
                "v1",
                tenantId,
                operatorId != null ? operatorId : "system",
                payload
        );

        try {
            String jsonPayload = objectMapper.writeValueAsString(envelope);
            String topic = "fees-invoice-lifecycle";

            log.info("[InvoiceEventPublisher] Enqueuing {} event to Outbox. EventId: {}, TenantId: {}", 
                    eventType, envelope.getEventId(), tenantId);

            outboxPublisher.enqueue(envelope.getEventId(), topic, jsonPayload, tenantId);
        } catch (Exception e) {
            log.error("[InvoiceEventPublisher] Error serializing EventEnvelope for type: {}", eventType, e);
            throw new RuntimeException("Failed to publish event: " + eventType, e);
        }
    }
}
