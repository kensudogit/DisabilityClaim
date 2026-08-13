package com.disabilityclaim.service;

import com.disabilityclaim.domain.entity.AuditLog;
import com.disabilityclaim.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(UUID actorUserId, String actorUsername, String action,
                       String entityType, String entityId,
                       String beforeValue, String afterValue, String ipAddress) {
        auditLogRepository.save(AuditLog.builder()
                .actorUserId(actorUserId)
                .actorUsername(maskIfNeeded(actorUsername))
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .beforeValue(maskSensitive(beforeValue))
                .afterValue(maskSensitive(afterValue))
                .ipAddress(ipAddress)
                .build());
    }

    private String maskIfNeeded(String value) {
        return value;
    }

    private String maskSensitive(String value) {
        if (value == null) {
            return null;
        }
        // Avoid logging password-like fields if present in JSON payloads
        return value.replaceAll("(?i)\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"");
    }
}
