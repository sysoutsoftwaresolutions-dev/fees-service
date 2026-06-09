package com.school.fees.controller;

import com.core.common.context.TenantContext;
import com.school.fees.model.FeeInvoice;
import com.school.fees.repository.FeeInvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller exposing GraphQL endpoints and fulfilling Apollo Federation v2 subgraph contracts for Fees Service.
 */
@Controller
public class FeesGraphQLController {

    private static final Logger log = LoggerFactory.getLogger(FeesGraphQLController.class);
    private final FeeInvoiceRepository invoiceRepository;

    @Value("classpath:graphql/schema.graphqls")
    private Resource schemaResource;

    private String schemaSdl;

    public FeesGraphQLController(FeeInvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @QueryMapping
    public FeeInvoice getInvoiceDetails(@Argument String invoiceId) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        log.info("[GraphQL] getInvoiceDetails called. TenantId: {}, InvoiceId: {}", tenantId, invoiceId);
        return invoiceRepository.findByTenantIdAndId(tenantId, invoiceId).orElse(null);
    }

    @SchemaMapping(typeName = "Student", field = "invoices")
    public List<FeeInvoice> getInvoicesForStudent(StudentRepresentation student) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }
        log.info("[GraphQL] Resolving invoices for Student ID: {}, TenantId: {}", student.getId(), tenantId);
        return invoiceRepository.findByTenantIdAndStudentId(tenantId, student.getId());
    }

    @QueryMapping
    public Map<String, String> _service() {
        if (schemaSdl == null) {
            try {
                schemaSdl = StreamUtils.copyToString(schemaResource.getInputStream(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("[GraphQL] Failed to load schema.graphqls for SDL query", e);
                schemaSdl = "";
            }
        }
        return Map.of("sdl", schemaSdl);
    }

    @QueryMapping
    public List<Object> _entities(@Argument List<Map<String, Object>> representations) {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "default";
        }

        log.info("[GraphQL] _entities called in Fees Service. Representations count: {}, TenantId: {}", representations.size(), tenantId);
        List<Object> results = new ArrayList<>();

        for (Map<String, Object> rep : representations) {
            String typename = (String) rep.get("__typename");
            if ("Student".equals(typename)) {
                String id = (String) rep.get("id");
                results.add(new StudentRepresentation(id));
            } else {
                results.add(null);
            }
        }
        return results;
    }

    public static class StudentRepresentation {
        private String id;
        public StudentRepresentation() {}
        public StudentRepresentation(String id) { this.id = id; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }
}
