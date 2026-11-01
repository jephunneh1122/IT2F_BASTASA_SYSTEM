# Water Refilling Station System (IT2F_BASTASA_SYSTEM)

Console application (Java 17, Maven, SQLite) for managing customers, water inventory,
orders, payments, deliveries, reports and activity logs.

## Run
```
mvn compile exec:java -Dexec.mainClass=com.yourdomain.Main
```
or run `com.yourdomain.Main` from your IDE. The database file `water_station.db` and the
`reports/` folder are created automatically in the project folder on first use.

## First login
A default administrator is created on the first run: **admin / admin123**.
New registrations (Administrator, Staff/Cashier, Delivery Staff) stay `PENDING` until an
administrator approves them from the Admin menu.

## Structure (3 packages: dao, model, ui)
```
src/main/java/com/yourdomain
├── Main.java                 startup only: init DB -> create DAOs/utilities -> Authentication.start()
├── dao
│   ├── DatabaseConnection    opens the SQLite connection
│   ├── DatabaseInitializer   creates all tables (+ default admin and sample products)
│   ├── UserDao               getAllUsers(), getPendingUsers(), approveUser(int), ...
│   ├── CustomerDao / ProductDao / OrderDao / AuditLogDao
│   └── AuditLogger, PasswordUtil
├── model                     User, Customer, Product, WaterOrder, SaleRecord, LogEntry,
│                             DailyReport, ReportSection
└── ui
    ├── Authentication        login, registration, role routing
    ├── AdminMenu             users, approvals, deactivation, logs + access to all modules
    ├── StaffMenu             customers, orders, payments, stock updates
    ├── DeliveryMenu          assigned deliveries + delivery status updates
    ├── CustomerMenu / InventoryMenu / OrderMenu / ReportMenu
    └── AppContext, ConsoleTable, InputHelper, ReportExporter
```

## Role permissions
| Feature | Admin | Staff/Cashier | Delivery |
|---|:-:|:-:|:-:|
| Approve / view / deactivate user accounts | yes | | |
| Add, view, update customers | yes | yes | |
| Deactivate customers | yes | | |
| View inventory, update stock quantity | yes | yes | |
| Update prices, add products | yes | | |
| Create / update orders, process payments | yes | yes | |
| Delete canceled or incorrect orders | yes | | |
| View delivery records | yes | yes | own only |
| Update delivery status | via order status | via order status | yes |
| Daily report + export (PDF / CSV / XLSX) | yes | | |
| Activity logs | yes | | |

## Moving this into an existing git clone
1. Delete the old source files in your clone (old `Main.java` and the old `dao`, `model`, `ui` folders).
2. Copy `src/main/java/com` from this project into your clone's `src/main/java/`.
3. Make sure your clone's `pom.xml` has Java 17 and the `org.xerial:sqlite-jdbc` dependency.
4. Copy `.gitignore` so `water_station.db`, `reports/` and `target/` are not committed.
5. `git add -A`, `git commit -m "Add water refilling station system"`, `git push`.
