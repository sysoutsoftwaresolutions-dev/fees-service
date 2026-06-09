package com.school.fees.event;

import java.util.UUID;

/**
 * Metadata envelope wrapper for all eventing payloads sent via Kafka by the Fees Service.
 */
public class EventEnvelope<T> {
    private String eventId;
    private String correlationId;
    private String eventType;
    private String eventVersion;
    private Long timestamp;
    private String tenantId;
    private String operatorId;
    private T payload;

    public EventEnvelope() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public EventEnvelope(String correlationId, String eventType, String eventVersion, String tenantId, String operatorId, T payload) {
        this();
        this.correlationId = correlationId;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.tenantId = tenantId;
        this.operatorId = operatorId;
        this.payload = payload;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventVersion() { return eventVersion; }
    public void setEventVersion(String eventVersion) { this.eventVersion = eventVersion; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }

    public T getPayload() { return payload; }
    public void setPayload(T payload) { this.payload = payload; }
}
