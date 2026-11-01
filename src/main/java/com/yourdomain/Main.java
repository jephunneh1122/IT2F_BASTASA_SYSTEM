package com.yourdomain;

import com.yourdomain.dao.AuditLogDao;
import com.yourdomain.dao.CustomerDao;
import com.yourdomain.dao.DatabaseInitializer;
import com.yourdomain.dao.OrderDao;
import com.yourdomain.dao.ProductDao;
import com.yourdomain.dao.UserDao;
import com.yourdomain.ui.AppContext;
import com.yourdomain.ui.Authentication;
import com.yourdomain.dao.AuditLogger;
import com.yourdomain.ui.ReportExporter;

import java.util.Scanner;

/**
 * Startup entry point only:
 * Initialize Database -> Instantiate DAOs & Utilities -> Authentication.start()
 */
public class Main {

    public static void main(String[] args) {
        // 1. Make sure all tables exist before any menu is shown
        DatabaseInitializer.initialize();

        // 2. Create the single shared instances
        Scanner scanner = new Scanner(System.in);
        UserDao userDao = new UserDao();
        CustomerDao customerDao = new CustomerDao();
        ProductDao productDao = new ProductDao();
        OrderDao orderDao = new OrderDao();
        AuditLogDao auditLogDao = new AuditLogDao();
        AuditLogger logger = new AuditLogger(auditLogDao);
        ReportExporter exporter = new ReportExporter();

        AppContext context = new AppContext(scanner, userDao, customerDao, productDao,
                orderDao, auditLogDao, logger, exporter);

        // 3. Hand over to the login / registration / role routing handler
        new Authentication(context).start();

        scanner.close();
    }
}
