package com.school.fees.migration;

import io.mongock.api.annotations.BeforeExecution;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackBeforeExecution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

/**
 * Mongock migration class for initializing collection indexes in the Fees Service database.
 */
@ChangeUnit(id = "fees-migration-init", order = "001", author = "antigravity")
public class DatabaseInitMigration {

    @BeforeExecution
    public void beforeExecution() {}

    @RollbackBeforeExecution
    public void rollbackBeforeExecution() {}

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        IndexOperations invoiceOps = mongoTemplate.indexOps("fee_invoices");
        invoiceOps.ensureIndex(new Index().on("tenantId", org.springframework.data.domain.Sort.Direction.ASC));
        invoiceOps.ensureIndex(new Index().on("studentId", org.springframework.data.domain.Sort.Direction.ASC));
        invoiceOps.ensureIndex(new Index().on("status", org.springframework.data.domain.Sort.Direction.ASC));
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps("fee_invoices").dropAllIndexes();
    }
}
