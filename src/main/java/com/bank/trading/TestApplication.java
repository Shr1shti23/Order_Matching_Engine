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
 * Console test harness for the trading engine.
 *
 * <p>This class is intentionally thin: it reads user input, constructs
 * domain objects, and delegates every operation to the service layer.
 * Zero business logic lives here.</p>
 *
 * <p>Authentication is bypassed.  The system auto-logs-in as the seeded
 * {@code sysadmin} user (user_id = 1) and all test traders use the IDs
 * seeded by {@code 08_test_data/01_traders_clients.sql}.</p>
 *
 * <p>This class must be removable once real authentication is integrated.</p>
 */
public final class TestApplication {

    // ------------------------------------------------------------------ //
    //  Seeded IDs (match 08_test_data/01_traders_clients.sql)            //
    // ------------------------------------------------------------------ //
    /** trader_arjun */   private static final long TRADER_1  = 2L;
    /** trader_priya */   private static final long TRADER_2  = 3L;
    /** client_sanjay */  private static final long CLIENT_1  = 7L;
    /** client_neha   */  private static final long CLIENT_2  = 8L;
    /** client_rahul  */  private static final long CLIENT_3  = 9L;

    // ------------------------------------------------------------------ //
    //  Services                                                           //
    // ------------------------------------------------------------------ //
    private static CacheManager        cache;
    private static TradingService      tradingService;
    private static CancelOrderService  cancelOrderService;
    private static ExpiryService       expiryService;
    private static ModifyOrderService  modifyOrderService;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Bank Trading Platform — Test Mode              ");
        System.out.println("  Auto-login: sysadmin (authentication bypassed) ");
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
        ReservationService   reservationService   = new ReservationService(cache);
        OrderBookService     orderBookService     = new OrderBookService(cache);
        MatchingEngine       matchingEngine       = new MatchingEngine();
        SettlementService    settlementService    = new SettlementService(
            cache, reservationService, orderBookService);
        RiskValidationService riskValidationService = new RiskValidationService(cache);
        cancelOrderService = new CancelOrderService(
            cache, reservationService, orderBookService);
        expiryService = new ExpiryService(cache, cancelOrderService);
        tradingService = new TradingService(
            cache, riskValidationService, reservationService,
            matchingEngine, settlementService, orderBookService, cancelOrderService);
        modifyOrderService = new ModifyOrderService(
            cache, tradingService, cancelOrderService, reservationService);

        System.out.println("\n[Ready] Startup complete. Type a menu number and press Enter.\n");

        // ------ Console loop ------
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1"  -> placeBuyOrder(scanner);
                case "2"  -> placeSellOrder(scanner);
                case "3"  -> cancelOrder(scanner);
                case "4"  -> modifyOrder(scanner);
                case "5"  -> viewOrderBook(scanner);
                case "6"  -> viewPortfolio(scanner);
                case "7"  -> viewRecentTrades(scanner);
                case "8"  -> viewWallet(scanner);
                case "9"  -> runExpiry(scanner);
                case "10" -> listInstruments();
                case "11" -> listActiveOrders(scanner);
                case "0"  -> running = false;
                default   -> System.out.println("  Unknown option. Try again.");
            }
        }

        System.out.println("\n[Shutdown] Closing database pool…");
        DatabaseConfig.close();
        System.out.println("[Shutdown] Done. Goodbye.");
    }

    // ------------------------------------------------------------------ //
    //  Menu                                                               //
    // ------------------------------------------------------------------ //

    private static void printMenu() {
        System.out.println("""

            ┌─────────────────────────────────────────┐
            │   TRADING ENGINE — MAIN MENU            │
            ├─────────────────────────────────────────┤
            │  1  Place BUY  order                    │
            │  2  Place SELL order                    │
            │  3  Cancel order                        │
            │  4  Modify order                        │
            │  5  View order book                     │
            │  6  View portfolio (holdings)           │
            │  7  View recent trades                  │
            │  8  View wallet balance                 │
            │  9  Run DAY order expiry                │
            │ 10  List instruments                    │
            │ 11  List active orders (in book)        │
            │  0  Exit                                │
            └─────────────────────────────────────────┘
            Choice: """);
    }

    // ------------------------------------------------------------------ //
    //  Menu handlers — all delegate to service layer                     //
    // ------------------------------------------------------------------ //

    private static void placeBuyOrder(Scanner sc) {
        System.out.println("\n-- Place BUY Order --");
        String symbol = promptSymbol(sc);
        if (symbol == null) return;

        long   traderId  = promptLong(sc, "Trader ID [2=arjun, 3=priya]: ");
        long   clientId  = promptLong(sc, "Client  ID [7=sanjay, 8=neha, 9=rahul]: ");
        String typeName  = promptChoice(sc, "Order type [LIMIT/MARKET]: ", "LIMIT", "MARKET");
        long   qty       = promptLong(sc, "Quantity: ");
        String tifName   = promptChoice(sc, "TIF [DAY/GTC/IOC/FOK]: ", "DAY", "GTC", "IOC", "FOK");

        Order order = new Order();
        order.setSymbol(symbol);
        order.setTraderId(traderId);
        order.setClientId(clientId);
        order.setInstrumentId(resolveInstrumentId(symbol));
        order.setSide(Side.BUY);
        order.setOrderType(OrderType.valueOf(typeName));
        order.setTimeInForce(TimeInForce.valueOf(tifName));
        order.setOriginalQty(qty);
        order.setRemainingQty(qty);

        if (order.getOrderType() == OrderType.LIMIT) {
            BigDecimal price = promptDecimal(sc, "Limit price: ");
            order.setPrice(price);
        }

        OrderPlacementResult result = tradingService.placeOrder(order);
        printResult(result);
    }

    private static void placeSellOrder(Scanner sc) {
        System.out.println("\n-- Place SELL Order --");
        String symbol = promptSymbol(sc);
        if (symbol == null) return;

        long   traderId  = promptLong(sc, "Trader ID [2=arjun, 3=priya]: ");
        long   clientId  = promptLong(sc, "Client  ID [7=sanjay, 8=neha, 9=rahul]: ");
        String typeName  = promptChoice(sc, "Order type [LIMIT/MARKET]: ", "LIMIT", "MARKET");
        long   qty       = promptLong(sc, "Quantity: ");
        String tifName   = promptChoice(sc, "TIF [DAY/GTC/IOC/FOK]: ", "DAY", "GTC", "IOC", "FOK");

        Order order = new Order();
        order.setSymbol(symbol);
        order.setTraderId(traderId);
        order.setClientId(clientId);
        order.setInstrumentId(resolveInstrumentId(symbol));
        order.setSide(Side.SELL);
        order.setOrderType(OrderType.valueOf(typeName));
        order.setTimeInForce(TimeInForce.valueOf(tifName));
        order.setOriginalQty(qty);
        order.setRemainingQty(qty);

        if (order.getOrderType() == OrderType.LIMIT) {
            BigDecimal price = promptDecimal(sc, "Limit price: ");
            order.setPrice(price);
        }

        OrderPlacementResult result = tradingService.placeOrder(order);
        printResult(result);
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
        OrderPlacementResult result = modifyOrderService.modify(orderId, newPrice, newQty, actorUserId);
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
            System.out.printf("  %-14s %-12s %-8s%n",
                e.getKey(), e.getValue().getTotalQuantity(), e.getValue().getOrderCount());
        }
        if (book.getBuyBook().isEmpty()) System.out.println("  (empty)");

        System.out.println("\n  --- SELL SIDE (best ask first) ---");
        System.out.printf("  %-14s %-12s %-8s%n", "Price", "Qty @ Level", "Orders");
        for (Map.Entry<BigDecimal, PriceLevel> e : book.getSellBook().entrySet()) {
            System.out.printf("  %-14s %-12s %-8s%n",
                e.getKey(), e.getValue().getTotalQuantity(), e.getValue().getOrderCount());
        }
        if (book.getSellBook().isEmpty()) System.out.println("  (empty)");
    }

    private static void viewPortfolio(Scanner sc) {
        System.out.println("\n-- View Portfolio --");
        long clientId = promptLong(sc, "Client ID: ");
        Map<Integer, Holding> holdings = cache.getHoldingsForClient(clientId);
        if (holdings.isEmpty()) {
            System.out.println("  No holdings for client " + clientId);
            return;
        }
        System.out.printf("  %-12s %-10s %-14s %-14s %-14s%n",
            "InstrumentId", "Qty", "Reserved", "AvgBuyPrice", "AvailableQty");
        for (Holding h : holdings.values()) {
            System.out.printf("  %-12d %-10d %-14d %-14s %-14d%n",
                h.getInstrumentId(), h.getQuantity(), h.getReservedQuantity(),
                h.getAvgBuyPrice(), h.getAvailableQuantity());
        }
    }

    private static void viewRecentTrades(Scanner sc) {
        System.out.println("\n-- Recent Trades --");
        String symbol = promptSymbol(sc);
        if (symbol == null) return;

        List<Trade> trades = cache.getRecentTrades(symbol);
        if (trades.isEmpty()) {
            System.out.println("  No recent trades for " + symbol);
            return;
        }
        System.out.printf("  %-10s %-14s %-10s %-22s%n", "TradeId", "Price", "Qty", "ExecutedAt");
        for (Trade t : trades) {
            System.out.printf("  %-10d %-14s %-10d %-22s%n",
                t.getTradeId(), t.getPrice(), t.getQuantity(), t.getExecutedAt());
        }
    }

    private static void viewWallet(Scanner sc) {
        System.out.println("\n-- Wallet Balance --");
        long clientId = promptLong(sc, "Client ID: ");
        Wallet wallet = cache.getWallet(clientId);
        if (wallet == null) {
            System.out.println("  No wallet for client " + clientId);
            return;
        }
        System.out.printf("  Wallet ID   : %d%n",       wallet.getWalletId());
        System.out.printf("  Currency    : %s%n",        wallet.getCurrency());
        System.out.printf("  Total Bal.  : %s%n",        wallet.getCashBalance());
        System.out.printf("  Reserved    : %s%n",        wallet.getReservedBalance());
        System.out.printf("  Available   : %s%n",        wallet.getAvailableBalance());
        System.out.printf("  Version     : %d%n",        wallet.getVersion());
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
        System.out.printf("  %-6s %-8s %-30s %-10s %-12s %-14s%n",
            "ID", "Symbol", "Name", "Type", "Tick", "LastPrice");
        for (Instrument i : cache.getAllInstruments()) {
            System.out.printf("  %-6d %-8s %-30s %-10s %-12s %-14s%n",
                i.getInstrumentId(), i.getSymbol(), i.getName(),
                i.getInstrumentType(), i.getTickSize(), i.getLastTradedPrice());
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

        System.out.printf("  %-10s %-8s %-8s %-14s %-12s %-8s %-12s%n",
            "OrderId", "Side", "Type", "Price", "RemQty", "TIF", "Client");

        printOrdersFromSide(book.getBuyBook());
        printOrdersFromSide(book.getSellBook());
    }

    private static void printOrdersFromSide(TreeMap<BigDecimal, PriceLevel> side) {
        for (PriceLevel level : side.values()) {
            OrderNode node = level.getHead();
            while (node != null) {
                Order o = node.getOrder();
                System.out.printf("  %-10d %-8s %-8s %-14s %-12d %-8s %-12d%n",
                    o.getOrderId(), o.getSide(), o.getOrderType(),
                    o.getPrice(), o.getRemainingQty(), o.getTimeInForce(), o.getClientId());
                node = node.getNext();
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  I/O helpers                                                        //
    // ------------------------------------------------------------------ //

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
            System.out.println("  [OK]   orderId=" + result.getOrderId()
                + " — " + result.getMessage());
        } else {
            System.out.println("  [FAIL] " + result.getMessage());
        }
    }
}
