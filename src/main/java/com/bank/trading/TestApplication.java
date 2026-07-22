package com.bank.trading;

import com.bank.trading.cache.CacheManager;
import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.engine.OrderBook;
import com.bank.trading.engine.OrderNode;
import com.bank.trading.engine.PriceLevel;
import com.bank.trading.model.*;
import com.bank.trading.service.*;
import com.bank.trading.util.OrderPlacementResult;

import java.math.BigDecimal;
import java.util.*;

/**
 * Console Application for Bank Trading Platform with Role-Based Access Control (RBAC).
 *
 * <p>Supports interactive Login for Admin, Trader, and Client user roles,
 * routing each authenticated user to their role-specific dashboard.</p>
 */
public final class TestApplication {

    private static CacheManager        cache;
    private static RBACService         rbacService;
    private static AdminService        adminService;
    private static TraderService       traderService;
    private static ClientService       clientService;
    private static ExpiryService       expiryService;
    private static CancelOrderService  cancelOrderService;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("     BANK TRADING PLATFORM — RBAC CONSOLE        ");
        System.out.println("=================================================");

        // ------ Initialise DB connection pool ------
        try {
            DatabaseConfig.init();
        } catch (Exception ex) {
            System.err.println("[FATAL] Cannot initialise database: " + ex.getMessage());
            System.exit(1);
        }

        // ------ Load all caches and rebuild order books ------
        cache = CacheManager.getInstance();
        cache.startup(50);

        // ------ Wire services ------
        ReservationService    reservationService    = new ReservationService(cache);
        OrderBookService      orderBookService      = new OrderBookService(cache);
        MatchingEngine        matchingEngine        = new MatchingEngine();
        SettlementService     settlementService     = new SettlementService(cache, reservationService, orderBookService);
        RiskValidationService riskValidationService  = new RiskValidationService(cache);
        cancelOrderService                          = new CancelOrderService(cache, reservationService, orderBookService);
        TradingService        tradingService        = new TradingService(cache, riskValidationService, reservationService, matchingEngine, settlementService, orderBookService, cancelOrderService);
        ModifyOrderService    modifyOrderService    = new ModifyOrderService(cache, tradingService, cancelOrderService, reservationService);

        rbacService   = new RBACService();
        adminService  = new AdminService(cache);
        traderService = new TraderService(cache, tradingService, cancelOrderService, modifyOrderService);
        clientService = new ClientService(cache);
        expiryService = new ExpiryService(cache, cancelOrderService);

        System.out.println("\n[Ready] System startup complete.\n");

        Scanner scanner = new Scanner(System.in);
        boolean appRunning = true;

        while (appRunning) {
            System.out.println("\n-------------------------------------------------");
            System.out.println("  SYSTEM LOGIN");
            System.out.println("-------------------------------------------------");
            System.out.println("  1. Login");
            System.out.println("  0. Exit System");
            System.out.print("  Choice: ");

            String choice = scanner.nextLine().trim();
            if ("0".equals(choice)) {
                appRunning = false;
                break;
            } else if (!"1".equals(choice)) {
                System.out.println("  Invalid choice. Please try again.");
                continue;
            }

            User currentUser = attemptLogin(scanner);
            if (currentUser != null) {
                switch (currentUser.getRoleId()) {
                    case 1 -> runAdminDashboard(scanner, currentUser);
                    case 2 -> runTraderDashboard(scanner, currentUser);
                    case 3 -> runClientDashboard(scanner, currentUser);
                    default -> System.out.println("  Unknown role ID: " + currentUser.getRoleId());
                }
            }
        }

        System.out.println("\n[Shutdown] Closing database pool…");
        DatabaseConfig.close();
        System.out.println("[Shutdown] Done. Goodbye.");
    }

    // ================================================================== //
    //  AUTHENTICATION                                                     //
    // ================================================================== //

    private static User attemptLogin(Scanner scanner) {
        System.out.print("\n  Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("  Password: ");
        String password = scanner.nextLine().trim();

        try {
            User user = rbacService.authenticate(username, password);
            String roleName = switch (user.getRoleId()) {
                case 1 -> "ADMIN";
                case 2 -> "TRADER";
                case 3 -> "CLIENT";
                default -> "USER";
            };
            System.out.println("\n  [SUCCESS] Welcome, " + user.getUsername() + " (" + roleName + ")");
            return user;
        } catch (Exception e) {
            System.out.println("  [LOGIN FAILED] " + e.getMessage());
            return null;
        }
    }

    // ================================================================== //
    //  ADMIN DASHBOARD (Hierarchical Menus)                               //
    // ================================================================== //

    private static void runAdminDashboard(Scanner sc, User admin) {
        boolean inAdminSession = true;
        while (inAdminSession) {
            System.out.println("""
                
                ┌────────────────────────────────────────────────────────┐
                │               ADMINISTRATION MAIN MENU                 │
                ├────────────────────────────────────────────────────────┤
                │  1  Trader Management                                  │
                │  2  Client Management                                  │
                │  3  Trader-Client Assignments                              │
                │  4  Instrument Management                              │
                │  5  System Monitoring & Expiry                         │
                │  0  Logout                                             │
                └────────────────────────────────────────────────────────┘
                Choice: """);

            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> runTraderManagementMenu(sc, admin);
                case "2" -> runClientManagementMenu(sc, admin);
                case "3" -> runAssignmentManagementMenu(sc, admin);
                case "4" -> runInstrumentManagementMenu(sc, admin);
                case "5" -> runSystemMonitoringMenu(sc, admin);
                case "0" -> inAdminSession = false;
                default  -> System.out.println("  Invalid choice. Please select 0-5.");
            }
        }
    }

    // --- Admin Sub-Menu 1: Trader Management ---
    private static void runTraderManagementMenu(Scanner sc, User admin) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("""
                
                ┌────────────────────────────────────────────────────────┐
                │                  TRADER MANAGEMENT                     │
                ├────────────────────────────────────────────────────────┤
                │  1  Create Trader                                      │
                │  2  Suspend Trader                                     │
                │  3  Activate Trader                                    │
                │  4  Delete Trader                                      │
                │  5  List All Traders                                   │
                │  0  Back to Admin Main Menu                            │
                └────────────────────────────────────────────────────────┘
                Choice: """);

            String input = sc.nextLine().trim();
            switch (input) {
                case "1" -> adminCreateTrader(sc, admin);
                case "2" -> adminSuspendTrader(sc, admin);
                case "3" -> adminActivateTrader(sc, admin);
                case "4" -> adminDeleteTrader(sc, admin);
                case "5" -> adminListTraders();
                case "0" -> inMenu = false;
                default  -> System.out.println("  Invalid choice.");
            }
        }
    }

    // --- Admin Sub-Menu 2: Client Management ---
    private static void runClientManagementMenu(Scanner sc, User admin) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("""
                
                ┌────────────────────────────────────────────────────────┐
                │                  CLIENT MANAGEMENT                     │
                ├────────────────────────────────────────────────────────┤
                │  1  Create Client                                      │
                │  2  Update Client Profile                              │
                │  3  Delete Client                                      │
                │  4  List All Clients                                   │
                │  5  Initialize Client Portfolio                        │
                │  0  Back to Admin Main Menu                            │
                └────────────────────────────────────────────────────────┘
                Choice: """);

            String input = sc.nextLine().trim();
            switch (input) {
                case "1" -> adminCreateClient(sc, admin);
                case "2" -> adminUpdateClient(sc);
                case "3" -> adminDeleteClient(sc, admin);
                case "4" -> adminListClients();
                case "5" -> adminInitHolding(sc, admin);
                case "0" -> inMenu = false;
                default  -> System.out.println("  Invalid choice.");
            }
        }
    }

    // --- Admin Sub-Menu 3: Trader-Client Assignments ---
    private static void runAssignmentManagementMenu(Scanner sc, User admin) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("""
                
                ┌────────────────────────────────────────────────────────┐
                │             TRADER-CLIENT ASSIGNMENTS                  │
                ├────────────────────────────────────────────────────────┤
                │  1  Assign Client to Trader                            │
                │  2  Reassign Client to New Trader                      │
                │  3  Remove Assignment                                  │
                │  4  List All Assignments                               │
                │  0  Back to Admin Main Menu                            │
                └────────────────────────────────────────────────────────┘
                Choice: """);

            String input = sc.nextLine().trim();
            switch (input) {
                case "1" -> adminAssignClient(sc, admin);
                case "2" -> adminReassignClient(sc, admin);
                case "3" -> adminRemoveAssignment(sc);
                case "4" -> adminListAssignments();
                case "0" -> inMenu = false;
                default  -> System.out.println("  Invalid choice.");
            }
        }
    }

    // --- Admin Sub-Menu 4: Instrument Management ---
    private static void runInstrumentManagementMenu(Scanner sc, User admin) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("""
                
                ┌────────────────────────────────────────────────────────┐
                │                INSTRUMENT MANAGEMENT                   │
                ├────────────────────────────────────────────────────────┤
                │  1  Register New Instrument                            │
                │  2  Update Instrument                                  │
                │  3  Activate Instrument                                │
                │  4  Deactivate Instrument                              │
                │  5  List All Instruments                               │
                │  0  Back to Admin Main Menu                            │
                └────────────────────────────────────────────────────────┘
                Choice: """);

            String input = sc.nextLine().trim();
            switch (input) {
                case "1" -> adminRegisterInstrument(sc, admin);
                case "2" -> adminUpdateInstrument(sc);
                case "3" -> adminActivateInstrument(sc);
                case "4" -> adminDeactivateInstrument(sc);
                case "5" -> adminListInstruments();
                case "0" -> inMenu = false;
                default  -> System.out.println("  Invalid choice.");
            }
        }
    }

    // --- Admin Sub-Menu 5: System Monitoring & Expiry ---
    private static void runSystemMonitoringMenu(Scanner sc, User admin) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("""
                
                ┌────────────────────────────────────────────────────────┐
                │             SYSTEM MONITORING & EXPIRY                 │
                ├────────────────────────────────────────────────────────┤
                │  1  View Live Order Book                               │
                │  2  View All Orders (Database)                         │
                │  3  View Active Orders (In Memory)                     │
                │  4  View All Executed Trades                           │
                │  5  View Audit Logs                                    │
                │  6  View System Dashboard Stats                        │
                │  7  Run DAY Order Expiry                               │
                │  0  Back to Admin Main Menu                            │
                └────────────────────────────────────────────────────────┘
                Choice: """);

            String input = sc.nextLine().trim();
            switch (input) {
                case "1" -> viewOrderBook(sc);
                case "2" -> adminViewAllOrders();
                case "3" -> listActiveOrders(sc);
                case "4" -> adminViewAllTrades();
                case "5" -> adminViewAuditLogs(sc);
                case "6" -> adminViewDashboardStats();
                case "7" -> runExpiry(sc);
                case "0" -> inMenu = false;
                default  -> System.out.println("  Invalid choice.");
            }
        }
    }

    // --- Admin Action Handlers ---

    private static void adminCreateTrader(Scanner sc, User admin) {
        System.out.println("\n-- Create Trader --");
        String username = prompt(sc, "Username: ").trim();
        String email    = prompt(sc, "Email: ").trim();
        String password = prompt(sc, "Password: ").trim();
        String empCode  = prompt(sc, "Employee Code: ").trim();
        String dept     = prompt(sc, "Department: ").trim();
        try {
            User u = adminService.createTrader(username, email, password, empCode, dept, admin.getUserId());
            System.out.println("  [OK] Created Trader user ID: " + u.getUserId());
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void adminSuspendTrader(Scanner sc, User admin) {
        long id = promptLong(sc, "Trader User ID to Suspend: ");
        try {
            adminService.suspendTrader(id, admin.getUserId());
            System.out.println("  [OK] Trader suspended.");
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void adminActivateTrader(Scanner sc, User admin) {
        long id = promptLong(sc, "Trader User ID to Activate: ");
        try {
            adminService.activateTrader(id, admin.getUserId());
            System.out.println("  [OK] Trader activated.");
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void adminDeleteTrader(Scanner sc, User admin) {
        long id = promptLong(sc, "Trader User ID to Delete: ");
        try {
            adminService.deleteTrader(id, admin.getUserId());
            System.out.println("  [OK] Trader marked DELETED.");
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void adminListTraders() {
        System.out.println("\n-- All Traders --");
        List<User> list = adminService.viewAllTraders();
        System.out.printf("  %-10s %-20s %-30s %-12s%n", "UserID", "Username", "Email", "Status");
        for (User u : list) {
            System.out.printf("  %-10d %-20s %-30s %-12s%n", u.getUserId(), u.getUsername(), u.getEmail(), u.getStatus());
        }
    }

    private static void adminCreateClient(Scanner sc, User admin) {
        System.out.println("\n-- Create Client --");
        String username = prompt(sc, "Username: ").trim();
        String email    = prompt(sc, "Email: ").trim();
        String password = prompt(sc, "Password: ").trim();
        String kyc      = promptChoice(sc, "KYC Status [VERIFIED/PENDING/REJECTED]: ", "VERIFIED", "PENDING", "REJECTED");
        String risk     = promptChoice(sc, "Risk Profile [CONSERVATIVE/MODERATE/AGGRESSIVE]: ", "MODERATE", "CONSERVATIVE", "AGGRESSIVE");
        BigDecimal bal  = promptDecimal(sc, "Initial Wallet Balance: ");
        String curr     = prompt(sc, "Currency [INR/USD]: ").trim().toUpperCase();

        try {
            User u = adminService.createClient(username, email, password, kyc, risk, bal, curr.isEmpty() ? "INR" : curr, admin.getUserId());
            System.out.println("  [OK] Created Client user ID: " + u.getUserId());
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void adminUpdateClient(Scanner sc) {
        long id     = promptLong(sc, "Client User ID: ");
        String kyc  = promptChoice(sc, "KYC Status [VERIFIED/PENDING/REJECTED]: ", "VERIFIED", "PENDING", "REJECTED");
        String risk = promptChoice(sc, "Risk Profile [CONSERVATIVE/MODERATE/AGGRESSIVE]: ", "MODERATE", "CONSERVATIVE", "AGGRESSIVE");
        try {
            adminService.updateClient(id, kyc, risk);
            System.out.println("  [OK] Client updated.");
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void adminDeleteClient(Scanner sc, User admin) {
        long id = promptLong(sc, "Client User ID to Delete: ");
        try {
            adminService.deleteClient(id, admin.getUserId());
            System.out.println("  [OK] Client marked DELETED.");
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void adminListClients() {
        System.out.println("\n-- All Clients --");
        List<User> list = adminService.viewAllClients();
        System.out.printf("  %-10s %-20s %-30s %-12s%n", "UserID", "Username", "Email", "Status");
        for (User u : list) {
            System.out.printf("  %-10d %-20s %-30s %-12s%n", u.getUserId(), u.getUsername(), u.getEmail(), u.getStatus());
        }
    }

    private static void adminInitHolding(Scanner sc, User admin) {
        System.out.println("\n-- Initialize Client Holding --");
        long clientId     = promptLong(sc, "Client ID: ");
        String symbol     = prompt(sc, "Symbol (e.g. AAPL): ").trim().toUpperCase();
        String instName   = prompt(sc, "Instrument Name: ").trim();
        String typeName   = promptChoice(sc, "Instrument Type [EQUITY/BOND/DERIVATIVE]: ", "EQUITY", "BOND", "DERIVATIVE");
        BigDecimal tick   = promptDecimal(sc, "Tick Size: ");
        long lot          = promptLong(sc, "Lot Size: ");
        long qty          = promptLong(sc, "Quantity: ");
        BigDecimal price  = promptDecimal(sc, "Avg Buy Price: ");

        try {
            adminService.initializeHolding(clientId, symbol, instName, InstrumentType.valueOf(typeName), tick, (int) lot, qty, price, admin.getUserId());
            System.out.println("  [OK] Initialized holding for client " + clientId);
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void adminAssignClient(Scanner sc, User admin) {
        long traderId = promptLong(sc, "Trader ID: ");
        long clientId = promptLong(sc, "Client ID: ");
        try {
            adminService.assignClientToTrader(traderId, clientId, admin.getUserId());
            System.out.println("  [OK] Assigned client " + clientId + " to trader " + traderId);
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void adminReassignClient(Scanner sc, User admin) {
        long oldId    = promptLong(sc, "Old Trader ID: ");
        long newId    = promptLong(sc, "New Trader ID: ");
        long clientId = promptLong(sc, "Client ID: ");
        try {
            adminService.reassignClient(oldId, newId, clientId, admin.getUserId());
            System.out.println("  [OK] Reassigned client " + clientId + " from trader " + oldId + " to " + newId);
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void adminRemoveAssignment(Scanner sc) {
        long traderId = promptLong(sc, "Trader ID: ");
        long clientId = promptLong(sc, "Client ID: ");
        try {
            adminService.removeAssignment(traderId, clientId);
            System.out.println("  [OK] Deassigned client " + clientId + " from trader " + traderId);
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void adminListAssignments() {
        System.out.println("\n-- Trader-Client Assignments --");
        List<TraderClientAssignment> list = adminService.viewAllAssignments();
        System.out.printf("  %-15s %-12s %-12s %-8s%n", "AssignmentID", "TraderID", "ClientID", "Active");
        for (TraderClientAssignment a : list) {
            System.out.printf("  %-15d %-12d %-12d %-8s%n", a.getAssignmentId(), a.getTraderId(), a.getClientId(), a.isActive());
        }
    }

    private static void adminRegisterInstrument(Scanner sc, User admin) {
        System.out.println("\n-- Register Instrument --");
        String symbol   = prompt(sc, "Symbol: ").trim().toUpperCase();
        String name     = prompt(sc, "Name: ").trim();
        String typeName = promptChoice(sc, "Type [EQUITY/BOND/DERIVATIVE]: ", "EQUITY", "BOND", "DERIVATIVE");
        BigDecimal tick = promptDecimal(sc, "Tick Size: ");
        long lot        = promptLong(sc, "Lot Size: ");
        try {
            Instrument i = adminService.registerInstrument(symbol, name, InstrumentType.valueOf(typeName), tick, (int) lot, admin.getUserId());
            System.out.println("  [OK] Registered instrument ID: " + i.getInstrumentId());
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void adminUpdateInstrument(Scanner sc) {
        int id          = (int) promptLong(sc, "Instrument ID: ");
        String name     = prompt(sc, "New Name: ").trim();
        String typeName = promptChoice(sc, "New Type [EQUITY/BOND/DERIVATIVE]: ", "EQUITY", "BOND", "DERIVATIVE");
        BigDecimal tick = promptDecimal(sc, "New Tick Size: ");
        long lot        = promptLong(sc, "New Lot Size: ");
        try {
            adminService.updateInstrument(id, name, InstrumentType.valueOf(typeName), tick, (int) lot);
            System.out.println("  [OK] Instrument updated.");
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void adminActivateInstrument(Scanner sc) {
        int id = (int) promptLong(sc, "Instrument ID: ");
        try {
            adminService.activateInstrument(id);
            System.out.println("  [OK] Instrument activated.");
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void adminDeactivateInstrument(Scanner sc) {
        int id = (int) promptLong(sc, "Instrument ID: ");
        try {
            adminService.deactivateInstrument(id);
            System.out.println("  [OK] Instrument deactivated.");
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void adminListInstruments() {
        System.out.println("\n-- All Registered Instruments --");
        List<Instrument> list = adminService.viewAllInstruments();
        System.out.printf("  %-6s %-8s %-30s %-12s %-10s %-12s%n", "ID", "Symbol", "Name", "Type", "Status", "LastPrice");
        for (Instrument i : list) {
            System.out.printf("  %-6d %-8s %-30s %-12s %-10s %-12s%n", i.getInstrumentId(), i.getSymbol(), i.getName(), i.getInstrumentType(), i.getStatus(), i.getLastTradedPrice());
        }
    }

    private static void adminViewAllOrders() {
        System.out.println("\n-- All Orders (DB) --");
        List<Order> list = adminService.viewAllOrders();
        System.out.printf("  %-10s %-8s %-8s %-8s %-12s %-10s %-12s %-14s%n", "OrderId", "Symbol", "Side", "Type", "Price", "Qty", "Status", "ClientId");
        for (Order o : list) {
            System.out.printf("  %-10d %-8s %-8s %-8s %-12s %-10d %-12s %-14d%n", o.getOrderId(), o.getSymbol(), o.getSide(), o.getOrderType(), o.getPrice(), o.getRemainingQty(), o.getStatus(), o.getClientId());
        }
    }

    private static void adminViewAllTrades() {
        System.out.println("\n-- All Executed Trades --");
        List<Trade> list = adminService.viewAllTrades();
        System.out.printf("  %-10s %-12s %-14s %-14s %-12s %-10s %-22s%n", "TradeId", "InstrumentId", "BuyOrderId", "SellOrderId", "Price", "Qty", "ExecutedAt");
        for (Trade t : list) {
            System.out.printf("  %-10d %-12d %-14d %-14d %-12s %-10d %-22s%n", t.getTradeId(), t.getInstrumentId(), t.getBuyOrderId(), t.getSellOrderId(), t.getPrice(), t.getQuantity(), t.getExecutedAt());
        }
    }

    private static void adminViewAuditLogs(Scanner sc) {
        long limit = promptLong(sc, "Limit [default 20]: ");
        int lim = limit <= 0 ? 20 : (int) limit;
        System.out.println("\n-- Audit Logs (Last " + lim + ") --");
        List<AuditLog> list = adminService.viewAuditLogs(lim);
        System.out.printf("  %-10s %-12s %-18s %-12s %-10s %-30s%n", "AuditID", "ActorUserID", "ActionType", "EntityType", "EntityID", "Details");
        for (AuditLog a : list) {
            System.out.printf("  %-10d %-12s %-18s %-12s %-10s %-30s%n", a.getAuditId(), a.getActorUserId(), a.getActionType(), a.getEntityType(), a.getEntityId(), a.getDetails());
        }
    }

    private static void adminViewDashboardStats() {
        System.out.println("\n-- System Dashboard Stats --");
        Map<String, Object> stats = adminService.viewDashboard();
        for (Map.Entry<String, Object> e : stats.entrySet()) {
            System.out.printf("  %-20s : %s%n", e.getKey(), e.getValue());
        }
    }

    // ================================================================== //
    //  TRADER DASHBOARD                                                   //
    // ================================================================== //

    private static void runTraderDashboard(Scanner sc, User trader) {
        boolean inSession = true;
        while (inSession) {
            System.out.println("""
                
                ┌────────────────────────────────────────────────────────┐
                │                   TRADER MAIN MENU                     │
                ├────────────────────────────────────────────────────────┤
                │  1  Order Operations                                   │
                │  2  Assigned Clients Management                        │
                │  0  Logout                                             │
                └────────────────────────────────────────────────────────┘
                Choice: """);

            String input = sc.nextLine().trim();
            switch (input) {
                case "1" -> runTraderOrderOperationsMenu(sc, trader);
                case "2" -> runTraderClientManagementMenu(sc, trader);
                case "0" -> inSession = false;
                default  -> System.out.println("  Invalid choice. Please select 0-2.");
            }
        }
    }

    private static void runTraderOrderOperationsMenu(Scanner sc, User trader) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("""
                
                ┌────────────────────────────────────────────────────────┐
                │                   ORDER OPERATIONS                     │
                ├────────────────────────────────────────────────────────┤
                │  1  Place BUY Order for Client                         │
                │  2  Place SELL Order for Client                        │
                │  3  Cancel Order                                       │
                │  4  Modify Order                                       │
                │  5  View Live Order Book                               │
                │  6  List Active Instruments                            │
                │  0  Back to Main Menu                                  │
                └────────────────────────────────────────────────────────┘
                Choice: """);

            String input = sc.nextLine().trim();
            switch (input) {
                case "1" -> traderPlaceOrder(sc, trader, Side.BUY);
                case "2" -> traderPlaceOrder(sc, trader, Side.SELL);
                case "3" -> cancelOrder(sc);
                case "4" -> modifyOrder(sc);
                case "5" -> viewOrderBook(sc);
                case "6" -> listInstruments();
                case "0" -> inMenu = false;
                default  -> System.out.println("  Invalid choice.");
            }
        }
    }

    private static void runTraderClientManagementMenu(Scanner sc, User trader) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("""
                
                ┌────────────────────────────────────────────────────────┐
                │             ASSIGNED CLIENTS MANAGEMENT                │
                ├────────────────────────────────────────────────────────┤
                │  1  View My Assigned Clients                           │
                │  2  View Client Portfolio                              │
                │  3  View Client Wallet Balance                         │
                │  4  View Client Orders                                 │
                │  5  View Client Trades                                 │
                │  0  Back to Main Menu                                  │
                └────────────────────────────────────────────────────────┘
                Choice: """);

            String input = sc.nextLine().trim();
            switch (input) {
                case "1" -> traderViewAssignedClients(trader);
                case "2" -> traderViewClientHoldings(sc, trader);
                case "3" -> traderViewClientWallet(sc, trader);
                case "4" -> traderViewClientOrders(sc, trader);
                case "5" -> traderViewClientTrades(sc, trader);
                case "0" -> inMenu = false;
                default  -> System.out.println("  Invalid choice.");
            }
        }
    }

    private static void traderPlaceOrder(Scanner sc, User trader, Side side) {
        System.out.println("\n-- Place " + side + " Order --");
        long clientId = promptLong(sc, "Client User ID: ");
        try {
            traderService.assertAssigned(trader.getUserId(), clientId);
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
            return;
        }

        String symbol = promptSymbol(sc);
        if (symbol == null) return;

        String typeName  = promptChoice(sc, "Order type [LIMIT/MARKET]: ", "LIMIT", "MARKET");
        long   qty       = promptLong(sc, "Quantity: ");
        String tifName   = promptChoice(sc, "TIF [DAY/GTC/IOC/FOK]: ", "DAY", "GTC", "IOC", "FOK");

        Order order = new Order();
        order.setSymbol(symbol);
        order.setTraderId(trader.getUserId());
        order.setClientId(clientId);
        order.setInstrumentId(resolveInstrumentId(symbol));
        order.setSide(side);
        order.setOrderType(OrderType.valueOf(typeName));
        order.setTimeInForce(TimeInForce.valueOf(tifName));
        order.setOriginalQty(qty);
        order.setRemainingQty(qty);

        if (order.getOrderType() == OrderType.LIMIT) {
            BigDecimal price = promptDecimal(sc, "Limit price: ");
            order.setPrice(price);
        }

        try {
            OrderPlacementResult result = traderService.placeOrder(trader.getUserId(), order);
            printResult(result);
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void traderViewAssignedClients(User trader) {
        System.out.println("\n-- Assigned Clients --");
        List<Long> clients = traderService.getAssignedClients(trader.getUserId());
        if (clients.isEmpty()) {
            System.out.println("  No clients currently assigned.");
            return;
        }
        System.out.println("  Client User IDs: " + clients);
    }

    private static void traderViewClientHoldings(Scanner sc, User trader) {
        long clientId = promptLong(sc, "Client User ID: ");
        try {
            Map<Integer, Holding> holdings = traderService.getClientHoldings(trader.getUserId(), clientId);
            printHoldingsMap(clientId, holdings);
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void traderViewClientWallet(Scanner sc, User trader) {
        long clientId = promptLong(sc, "Client User ID: ");
        try {
            Wallet w = traderService.getClientWallet(trader.getUserId(), clientId);
            printWalletDetails(clientId, w);
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void traderViewClientOrders(Scanner sc, User trader) {
        long clientId = promptLong(sc, "Client User ID: ");
        try {
            List<Order> orders = traderService.getClientOrders(trader.getUserId(), clientId);
            printOrdersList(clientId, orders);
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    private static void traderViewClientTrades(Scanner sc, User trader) {
        long clientId = promptLong(sc, "Client User ID: ");
        try {
            List<Trade> trades = traderService.getClientTrades(trader.getUserId(), clientId);
            printTradesList(clientId, trades);
        } catch (Exception e) {
            System.out.println("  [FAIL] " + e.getMessage());
        }
    }

    // ================================================================== //
    //  CLIENT DASHBOARD (Self-Service Read-Only)                          //
    // ================================================================== //

    private static void runClientDashboard(Scanner sc, User client) {
        boolean inSession = true;
        while (inSession) {
            System.out.println("""
                
                ┌────────────────────────────────────────────────────────┐
                │                   CLIENT MAIN MENU                     │
                ├────────────────────────────────────────────────────────┤
                │  1  Portfolio & Wallet                                 │
                │  2  Orders & Activity                                  │
                │  0  Logout                                             │
                └────────────────────────────────────────────────────────┘
                Choice: """);

            String input = sc.nextLine().trim();
            switch (input) {
                case "1" -> runClientPortfolioWalletMenu(sc, client);
                case "2" -> runClientOrdersActivityMenu(sc, client);
                case "0" -> inSession = false;
                default  -> System.out.println("  Invalid choice. Please select 0-2.");
            }
        }
    }

    private static void runClientPortfolioWalletMenu(Scanner sc, User client) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("""
                
                ┌────────────────────────────────────────────────────────┐
                │                  PORTFOLIO & WALLET                    │
                ├────────────────────────────────────────────────────────┤
                │  1  View My Portfolio (Holdings)                       │
                │  2  View My Wallet Balance                             │
                │  3  View My Wallet Transactions                        │
                │  0  Back to Main Menu                                  │
                └────────────────────────────────────────────────────────┘
                Choice: """);

            String input = sc.nextLine().trim();
            switch (input) {
                case "1" -> printHoldingsMap(client.getUserId(), clientService.getHoldings(client.getUserId()));
                case "2" -> printWalletDetails(client.getUserId(), clientService.getWallet(client.getUserId()));
                case "3" -> clientViewTransactions(client);
                case "0" -> inMenu = false;
                default  -> System.out.println("  Invalid choice.");
            }
        }
    }

    private static void runClientOrdersActivityMenu(Scanner sc, User client) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("""
                
                ┌────────────────────────────────────────────────────────┐
                │                  ORDERS & ACTIVITY                     │
                ├────────────────────────────────────────────────────────┤
                │  1  View My Orders                                     │
                │  2  View My Trades                                      │
                │  3  View My Profile                                    │
                │  4  List Active Instruments                            │
                │  0  Back to Main Menu                                  │
                └────────────────────────────────────────────────────────┘
                Choice: """);

            String input = sc.nextLine().trim();
            switch (input) {
                case "1" -> printOrdersList(client.getUserId(), clientService.getOrders(client.getUserId()));
                case "2" -> printTradesList(client.getUserId(), clientService.getTrades(client.getUserId()));
                case "3" -> clientViewProfile(client);
                case "4" -> listInstruments();
                case "0" -> inMenu = false;
                default  -> System.out.println("  Invalid choice.");
            }
        }
    }

    private static void clientViewTransactions(User client) {
        System.out.println("\n-- Wallet Transactions --");
        List<WalletTransaction> txns = clientService.getWalletTransactions(client.getUserId());
        if (txns.isEmpty()) {
            System.out.println("  No wallet transactions found.");
            return;
        }
        System.out.printf("  %-10s %-12s %-14s %-14s %-20s%n", "TxnID", "Type", "Amount", "BalanceAfter", "Reference");
        for (WalletTransaction t : txns) {
            System.out.printf("  %-10d %-12s %-14s %-14s %-20s%n", t.getTransactionId(), t.getTransactionType(), t.getAmount(), t.getBalanceAfter(), t.getReference());
        }
    }

    private static void clientViewProfile(User client) {
        System.out.println("\n-- Profile Details --");
        Optional<User> uOpt = clientService.getProfile(client.getUserId());
        if (uOpt.isPresent()) {
            User u = uOpt.get();
            System.out.println("  User ID  : " + u.getUserId());
            System.out.println("  Username : " + u.getUsername());
            System.out.println("  Email    : " + u.getEmail());
            System.out.println("  Status   : " + u.getStatus());
        } else {
            System.out.println("  Profile not found.");
        }
    }

    // ================================================================== //
    //  COMMON DISPLAY & INPUT HELPERS                                     //
    // ================================================================== //

    private static void printHoldingsMap(long clientId, Map<Integer, Holding> holdings) {
        System.out.println("\n-- Portfolio Holdings (Client " + clientId + ") --");
        if (holdings == null || holdings.isEmpty()) {
            System.out.println("  No holdings for client " + clientId);
            return;
        }
        System.out.printf("  %-12s %-10s %-14s %-14s %-14s%n", "InstrumentId", "Qty", "Reserved", "AvgBuyPrice", "AvailableQty");
        for (Holding h : holdings.values()) {
            System.out.printf("  %-12d %-10d %-14d %-14s %-14d%n", h.getInstrumentId(), h.getQuantity(), h.getReservedQuantity(), h.getAvgBuyPrice(), h.getAvailableQuantity());
        }
    }

    private static void printWalletDetails(long clientId, Wallet wallet) {
        System.out.println("\n-- Wallet Balance (Client " + clientId + ") --");
        if (wallet == null) {
            System.out.println("  No wallet found for client " + clientId);
            return;
        }
        System.out.printf("  Wallet ID   : %d%n", wallet.getWalletId());
        System.out.printf("  Currency    : %s%n", wallet.getCurrency());
        System.out.printf("  Total Bal.  : %s%n", wallet.getCashBalance());
        System.out.printf("  Reserved    : %s%n", wallet.getReservedBalance());
        System.out.printf("  Available   : %s%n", wallet.getAvailableBalance());
        System.out.printf("  Version     : %d%n", wallet.getVersion());
    }

    private static void printOrdersList(long clientId, List<Order> orders) {
        System.out.println("\n-- Orders (Client " + clientId + ") --");
        if (orders == null || orders.isEmpty()) {
            System.out.println("  No orders found for client " + clientId);
            return;
        }
        System.out.printf("  %-10s %-8s %-8s %-8s %-12s %-10s %-12s%n", "OrderId", "Symbol", "Side", "Type", "Price", "Qty", "Status");
        for (Order o : orders) {
            System.out.printf("  %-10d %-8s %-8s %-8s %-12s %-10d %-12s%n", o.getOrderId(), o.getSymbol(), o.getSide(), o.getOrderType(), o.getPrice(), o.getRemainingQty(), o.getStatus());
        }
    }

    private static void printTradesList(long clientId, List<Trade> trades) {
        System.out.println("\n-- Trades (Client " + clientId + ") --");
        if (trades == null || trades.isEmpty()) {
            System.out.println("  No trades found for client " + clientId);
            return;
        }
        System.out.printf("  %-10s %-12s %-14s %-10s %-22s%n", "TradeId", "InstrumentId", "Price", "Qty", "ExecutedAt");
        for (Trade t : trades) {
            System.out.printf("  %-10d %-12d %-14s %-10d %-22s%n", t.getTradeId(), t.getInstrumentId(), t.getPrice(), t.getQuantity(), t.getExecutedAt());
        }
    }

    private static void cancelOrder(Scanner sc) {
        System.out.println("\n-- Cancel Order --");
        long orderId     = promptLong(sc, "Order ID: ");
        long actorUserId = promptLong(sc, "Your User ID (actor): ");
        try {
            cancelOrderService.cancel(orderId, actorUserId);
            System.out.println("  [OK] Order " + orderId + " cancelled.");
        } catch (Exception ex) {
            System.out.println("  [FAIL] " + ex.getMessage());
        }
    }

    private static void modifyOrder(Scanner sc) {
        System.out.println("\n-- Modify Order --");
        long orderId     = promptLong(sc, "Order ID: ");
        long actorUserId = promptLong(sc, "Your User ID (actor): ");
        String priceInput = prompt(sc, "New price (Enter to keep same): ").trim();
        long newQty      = promptLong(sc, "New quantity: ");

        BigDecimal newPrice = priceInput.isEmpty() ? null : new BigDecimal(priceInput);
        OrderPlacementResult result = traderService.modifyOrder(orderId, newPrice, newQty, actorUserId);
        printResult(result);
    }

    private static void viewOrderBook(Scanner sc) {
        System.out.println("\n-- View Order Book --");
        String symbol = promptSymbol(sc);
        if (symbol == null) return;

        OrderBook book = cache.getOrderBook(symbol);
        if (book == null) {
            System.out.println("  No order book for " + symbol);
            return;
        }

        System.out.println("\n  ===== " + symbol + " ORDER BOOK =====");
        System.out.println("\n  --- BUY SIDE (best bid first) ---");
        System.out.printf("  %-14s %-12s %-8s%n", "Price", "Qty @ Level", "Orders");
        for (Map.Entry<BigDecimal, PriceLevel> e : book.getBuyBook().entrySet()) {
            System.out.printf("  %-14s %-12s %-8s%n", e.getKey(), e.getValue().getTotalQuantity(), e.getValue().getOrderCount());
        }
        if (book.getBuyBook().isEmpty()) System.out.println("  (empty)");

        System.out.println("\n  --- SELL SIDE (best ask first) ---");
        System.out.printf("  %-14s %-12s %-8s%n", "Price", "Qty @ Level", "Orders");
        for (Map.Entry<BigDecimal, PriceLevel> e : book.getSellBook().entrySet()) {
            System.out.printf("  %-14s %-12s %-8s%n", e.getKey(), e.getValue().getTotalQuantity(), e.getValue().getOrderCount());
        }
        if (book.getSellBook().isEmpty()) System.out.println("  (empty)");
    }

    private static void runExpiry(Scanner sc) {
        System.out.println("\n-- Run DAY Order Expiry --");
        System.out.print("  Symbol (or ALL): ");
        String input = sc.nextLine().trim().toUpperCase();
        int count;
        if (input.equals("ALL")) {
            count = expiryService.expireAll();
        } else {
            count = expiryService.expireBySymbol(input);
        }
        System.out.println("  [Done] Expired " + count + " order(s).");
    }

    private static void listInstruments() {
        System.out.println("\n-- Active Instruments --");
        System.out.printf("  %-6s %-8s %-30s %-10s %-12s %-14s%n", "ID", "Symbol", "Name", "Type", "Tick", "LastPrice");
        for (Instrument i : cache.getAllInstruments()) {
            System.out.printf("  %-6d %-8s %-30s %-10s %-12s %-14s%n", i.getInstrumentId(), i.getSymbol(), i.getName(), i.getInstrumentType(), i.getTickSize(), i.getLastTradedPrice());
        }
    }

    private static void listActiveOrders(Scanner sc) {
        System.out.println("\n-- Active Orders In Book --");
        String symbol = promptSymbol(sc);
        if (symbol == null) return;

        OrderBook book = cache.getOrderBook(symbol);
        if (book == null) {
            System.out.println("  No order book for " + symbol);
            return;
        }

        System.out.printf("  %-10s %-8s %-8s %-14s %-12s %-8s %-12s%n", "OrderId", "Side", "Type", "Price", "RemQty", "TIF", "Client");
        printOrdersFromSide(book.getBuyBook());
        printOrdersFromSide(book.getSellBook());
    }

    private static void printOrdersFromSide(TreeMap<BigDecimal, PriceLevel> side) {
        for (PriceLevel level : side.values()) {
            OrderNode node = level.getHead();
            while (node != null) {
                Order o = node.getOrder();
                System.out.printf("  %-10d %-8s %-8s %-14s %-12d %-8s %-12d%n", o.getOrderId(), o.getSide(), o.getOrderType(), o.getPrice(), o.getRemainingQty(), o.getTimeInForce(), o.getClientId());
                node = node.getNext();
            }
        }
    }

    private static String promptSymbol(Scanner sc) {
        System.out.print("  Symbol (e.g. AAPL, MSFT, TSLA, GOOGL): ");
        String s = sc.nextLine().trim().toUpperCase();
        if (cache.getInstrument(s) == null) {
            System.out.println("  Unknown symbol: " + s);
            return null;
        }
        return s;
    }

    private static long promptLong(Scanner sc, String label) {
        System.out.print("  " + label);
        try {
            return Long.parseLong(sc.nextLine().trim());
        } catch (NumberFormatException ex) {
            System.out.println("  Invalid number — defaulting to 0.");
            return 0L;
        }
    }

    private static BigDecimal promptDecimal(Scanner sc, String label) {
        System.out.print("  " + label);
        try {
            return new BigDecimal(sc.nextLine().trim());
        } catch (NumberFormatException ex) {
            System.out.println("  Invalid number — defaulting to 0.");
            return BigDecimal.ZERO;
        }
    }

    private static String prompt(Scanner sc, String label) {
        System.out.print("  " + label);
        return sc.nextLine();
    }

    private static String promptChoice(Scanner sc, String label, String... options) {
        System.out.print("  " + label);
        String input = sc.nextLine().trim().toUpperCase();
        for (String opt : options) {
            if (opt.equalsIgnoreCase(input)) return opt;
        }
        System.out.println("  Unrecognised — defaulting to " + options[0]);
        return options[0];
    }

    private static int resolveInstrumentId(String symbol) {
        Instrument i = cache.getInstrument(symbol);
        return (i != null) ? i.getInstrumentId() : 0;
    }

    private static void printResult(OrderPlacementResult result) {
        if (result.isSuccess()) {
            System.out.println("  [OK]   orderId=" + result.getOrderId() + " — " + result.getMessage());
        } else {
            System.out.println("  [FAIL] " + result.getMessage());
        }
    }
}
