/**
 * Append-only, hash-chained audit log shared across services.
 *
 * Each service owning a JPA store gets its own audit_log table (created by Flyway
 * in that service) and runs its own chain. The hash chain makes any post-hoc
 * tampering visible to {@link com.bank.common.audit.AuditChainVerifier} which runs
 * on a schedule.
 */
package com.bank.common.audit;
