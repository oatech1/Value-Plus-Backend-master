package com.valueplus.domain.service.concretes;

import com.valueplus.domain.enums.ActionType;
import com.valueplus.domain.enums.EntityType;
import com.valueplus.domain.model.AuditLogModel;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditResponse {

    private ActionType action;
    private EntityType entityType;
    private LocalDateTime createdAt;
    private AuditLogModel.ActorDetails actor;
    private String description;

    @Value
    public static class ActorDetails {
        Long userId;
        String email;
        String firstname;
        String lastname;
        String photo;
    }
}
