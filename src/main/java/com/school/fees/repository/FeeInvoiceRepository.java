package com.school.fees.repository;

import com.school.fees.model.FeeInvoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.List;

/**
 * MongoDB repository for Fee Invoices.
 */
public interface FeeInvoiceRepository extends MongoRepository<FeeInvoice, String> {
    Optional<FeeInvoice> findByTenantIdAndId(String tenantId, String id);
    List<FeeInvoice> findByTenantIdAndStudentId(String tenantId, String studentId);
    List<FeeInvoice> findByTenantId(String tenantId);
}
