package com.valueplus.domain.service.abstracts;

import com.valueplus.domain.model.AuditLogModel;
import com.valueplus.domain.model.AuditModel;
import com.valueplus.domain.service.concretes.AuditResponse;
import com.valueplus.persistence.entity.Account;
import com.valueplus.persistence.entity.AuditLog;
import com.valueplus.persistence.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditService  {
    void save(AuditLogModel model) throws Exception;

    Page<AuditLogModel> query(AuditModel model, Pageable pageable) throws Exception;

    Page<AuditResponse> fetchAudit(Pageable pageable);


}
