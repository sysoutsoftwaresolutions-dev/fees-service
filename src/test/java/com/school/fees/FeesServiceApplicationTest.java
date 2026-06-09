package com.school.fees;

import com.core.common.context.TenantContext;
import com.core.common.event.EventProcessingGuard;
import com.core.common.security.JwtTokenParser;
import com.mongodb.client.MongoClient;
import com.school.fees.consumer.StudentLifecycleEventConsumer;
import com.school.fees.model.FeeInvoice;
import com.school.fees.repository.FeeInvoiceRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Fees Service microservice.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "mongock.enabled=false"
})
public class FeesServiceApplicationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtTokenParser jwtTokenParser;

    @Autowired
    private StudentLifecycleEventConsumer eventConsumer;

    @MockBean
    private MongoClient mongoClient;

    @MockBean
    private com.core.common.event.OutboxPublisher outboxPublisher;

    @MockBean
    private EventProcessingGuard eventProcessingGuard;

    @MockBean
    private FeeInvoiceRepository invoiceRepository;

    @Test
    public void contextLoads() {
        assertNotNull(jwtTokenParser);
    }

    @Test
    public void testOpenApiEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/v3/api-docs", String.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("openapi"));
        assertTrue(response.getBody().contains("Fees API"));
    }

    @Test
    public void testSecuredWorkflowAspectDeny() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", "schoola");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/fees/invoices/any-id/pay",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertTrue(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStudentCreatedConsumerIdempotencyAndPublish() {
        Acknowledgment mockAck = Mockito.mock(Acknowledgment.class);
        
        Mockito.when(eventProcessingGuard.shouldProcess(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);

        String jsonEvent = "{\n" +
                "  \"eventId\": \"e123\",\n" +
                "  \"correlationId\": \"c123\",\n" +
                "  \"eventType\": \"StudentCreated\",\n" +
                "  \"eventVersion\": \"v1\",\n" +
                "  \"timestamp\": 1623120000000,\n" +
                "  \"tenantId\": \"schoolpremium\",\n" +
                "  \"operatorId\": \"op123\",\n" +
                "  \"payload\": {\n" +
                "    \"studentId\": \"stu999\",\n" +
                "    \"rollNumber\": \"R-100200\"\n" +
                "  }\n" +
                "}";

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "admission-student-lifecycle", 0, 0L, "e123", jsonEvent
        );

        eventConsumer.consumeStudentCreated(record, mockAck);

        Mockito.verify(mockAck, Mockito.times(1)).acknowledge();
        Mockito.verify(eventProcessingGuard, Mockito.times(1))
                .shouldProcess("schoolpremium", "e123");

        ArgumentCaptor<FeeInvoice> invoiceCaptor = ArgumentCaptor.forClass(FeeInvoice.class);
        Mockito.verify(invoiceRepository, Mockito.times(1)).save(invoiceCaptor.capture());
        FeeInvoice saved = invoiceCaptor.getValue();
        assertEquals("schoolpremium", saved.getTenantId());
        assertEquals("stu999", saved.getStudentId());
        Mockito.verify(outboxPublisher, Mockito.times(1))
                .enqueue(Mockito.anyString(), Mockito.eq("fees-invoice-lifecycle"), Mockito.anyString(), Mockito.eq("schoolpremium"));
    }
}
