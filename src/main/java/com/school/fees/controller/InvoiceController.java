package com.school.fees.controller;

import com.core.common.context.TenantContext;
import com.core.common.exception.CoreException;
import com.core.common.security.SecuredWorkflow;
import com.school.fees.event.FeePaidEventPayload;
import com.school.fees.event.InvoiceEventPublisher;
import com.school.fees.model.FeeInvoice;
import com.school.fees.repository.FeeInvoiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing fee invoices.
 */
@RestController
@RequestMapping("/api/fees")
@Tag(name = "Fees API", description = "Endpoints for managing fee invoices and payments")
public class InvoiceController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

    private final FeeInvoiceRepository invoiceRepository;
    private final InvoiceEventPublisher eventPublisher;

    public InvoiceController(FeeInvoiceRepository invoiceRepository, InvoiceEventPublisher eventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/invoices/{id}/pay")
    @SecuredWorkflow({"ADMIN", "STAFF"})
    @Operation(summary = "Pay a fee invoice", description = "Marks an invoice as PAID and dispatches a FeePaid event.")
    @ApiResponse(responseCode = "200", description = "Invoice paid successfully")
    public ResponseEntity<Map<String, Object>> payInvoice(
            @PathVariable("id") String invoiceId,
            @RequestHeader(value = "X-Operator-ID", defaultValue = "system") String operatorId) throws CoreException {

        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }

        log.info("[FeesAPI] Paying invoice. TenantId: {}, InvoiceId: {}, OperatorId: {}", 
                tenantId, invoiceId, operatorId);

        FeeInvoice invoice = invoiceRepository.findByTenantIdAndId(tenantId, invoiceId)
                .orElseThrow(() -> new CoreException("Invoice not found: " + invoiceId, "INVOICE_NOT_FOUND"));

        if ("PAID".equalsIgnoreCase(invoice.getStatus())) {
            throw new CoreException("Invoice is already paid", "INVOICE_ALREADY_PAID");
        }

        // Mark as paid
        invoice.setStatus("PAID");
        FeeInvoice saved = invoiceRepository.save(invoice);

        // Publish FeePaid event
        FeePaidEventPayload payload = new FeePaidEventPayload(
                saved.getId(),
                saved.getStudentId(),
                saved.getAmount(),
                System.currentTimeMillis()
        );
        eventPublisher.publishFeePaid(payload, null, operatorId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Invoice paid successfully");
        response.put("invoiceId", saved.getId());
        response.put("status", saved.getStatus());
        response.put("amountPaid", saved.getAmount());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/invoices")
    @SecuredWorkflow({"ADMIN", "STAFF"})
    @Operation(summary = "Get all invoices", description = "Retrieve list of all invoices for the current tenant.")
    public ResponseEntity<List<FeeInvoice>> getInvoices() {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        return ResponseEntity.ok(invoiceRepository.findByTenantId(tenantId));
    }

    @GetMapping("/invoices/student/{studentId}")
    @SecuredWorkflow({"ADMIN", "STAFF"})
    @Operation(summary = "Get invoices by student", description = "Retrieve invoices for a specific student.")
    public ResponseEntity<List<FeeInvoice>> getInvoicesByStudent(@PathVariable("studentId") String studentId) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        return ResponseEntity.ok(invoiceRepository.findByTenantIdAndStudentId(tenantId, studentId));
    }
}
