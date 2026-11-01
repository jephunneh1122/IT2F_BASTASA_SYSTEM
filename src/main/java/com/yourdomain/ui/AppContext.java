package com.yourdomain.ui;

import com.yourdomain.dao.AuditLogDao;
import com.yourdomain.dao.CustomerDao;
import com.yourdomain.dao.OrderDao;
import com.yourdomain.dao.ProductDao;
import com.yourdomain.dao.UserDao;
import com.yourdomain.dao.AuditLogger;

import java.util.Scanner;

/**
 * Holds the single shared Scanner, DAOs and utilities. It is created once in Main
 * and handed to every menu, so no menu ever creates its own Scanner or DAO.
 */
public record AppContext(Scanner scanner,
                         UserDao userDao,
                         CustomerDao customerDao,
                         ProductDao productDao,
                         OrderDao orderDao,
                         AuditLogDao auditLogDao,
                         AuditLogger logger,
                         ReportExporter exporter) {
}
