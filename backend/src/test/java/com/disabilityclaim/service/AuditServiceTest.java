package com.disabilityclaim.service;

import com.disabilityclaim.domain.entity.AuditLog;
import com.disabilityclaim.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void recordPersistsAuditLog() {
        UUID actorId = UUID.randomUUID();
        auditService.record(actorId, "admin", "CREATE_BATCH", "BillingBatch", "batch-1",
                null, "2026-08", "127.0.0.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getActorUserId()).isEqualTo(actorId);
        assertThat(log.getActorUsername()).isEqualTo("admin");
        assertThat(log.getAction()).isEqualTo("CREATE_BATCH");
        assertThat(log.getEntityType()).isEqualTo("BillingBatch");
        assertThat(log.getEntityId()).isEqualTo("batch-1");
        assertThat(log.getAfterValue()).isEqualTo("2026-08");
        assertThat(log.getIpAddress()).isEqualTo("127.0.0.1");
    }
}
