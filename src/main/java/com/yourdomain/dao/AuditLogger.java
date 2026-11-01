package com.yourdomain.dao;

import com.yourdomain.model.User;

/**
 * Small helper every menu uses to write to the User Activity Log.
 */
public class AuditLogger {

    private final AuditLogDao auditLogDao;

    public AuditLogger(AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    public void log(User user, String action, String details) {
        auditLogDao.insert(user.username(), action, details);
    }

    public void log(String username, String action, String details) {
        auditLogDao.insert(username, action, details);
    }
}
