package vdt.se.demo.domain.model;

import vdt.se.demo.domain.valueObjects.AuditStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    private UUID id;
    private String userIdentity;
    private LocalDateTime timestamp;
    private String nlQuery;
    private String generatedDSL;
    private Integer resultsCount;
    private Long executionTimeMs;
    private AuditStatus status;
    private String llmProvider;
    private String errorMessage;
}
