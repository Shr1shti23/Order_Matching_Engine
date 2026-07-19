package com.bank.trading.cache;

import com.bank.trading.dao.*;
import com.bank.trading.dao.impl.*;
import com.bank.trading.engine.OrderBook;
import com.bank.trading.engine.OrderNode;
import com.bank.trading.model.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Singleton runtime cache.
 *
 * <p>Holds all in-memory state. Every field is a plain HashMap or ArrayList —
 * no concurrent collections, no locks.  The application is single-threaded.</p>
 *
 * <p>Reservations (reservedBalance, reservedQuantity) are stored here only and
 * are recalculated from open orders every time {@link #startup()} is called.</p>
 */
public final class CacheManager {

    // ------------------------------------------------------------------ //
    //  Singleton                                                          //
    // ------------------------------------------------------------------ //

    private static CacheManager instance;

    public static CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }

    private CacheManager() {}

    // ------------------------------------------------------------------ //
    //  DAO references (set during startup)                                //
    // ------------------------------------------------------------------ //

    private InstrumentDao        instrumentDao;
    private WalletDao            walletDao;
    private HoldingDao           holdingDao;
    private TraderAssignmentDao  traderAssignmentDao;
    private TradeDao             tradeDao;
    private OrderDao             orderDao;

    // ------------------------------------------------------------------ //
    //  Runtime caches                                                     //
    // ------------------------------------------------------------------ //

    /** symbol → Instrument */
    private final HashMap<String, Instrument> instrumentCache = new HashMap<>();

    /** clientId → Wallet (includes in-memory reservedBalance) */
    private final HashMap<Long, Wallet> walletCache = new HashMap<>();

    /** clientId → (instrumentId → Holding) (includes in-memory reservedQuantity) */
    private final HashMap<Long, HashMap<Integer, Holding>> holdingCache = new HashMap<>();

    /** traderId → Set<clientId> */
    private final HashMap<Long, HashSet<Long>> traderAssignments = new HashMap<>();

    /** symbol → capped list of recent trades (max = recentTradesCap) */
    private final HashMap<String, ArrayList<Trade>> recentTradesCache = new HashMap<>();

    /** symbol → OrderBook */
    private final HashMap<String, OrderBook> orderBooks = new HashMap<>();

    /** orderId → OrderNode — O(1) lookup for cancel / modify */
    private final HashMap<Long, OrderNode> orderNodeMap = new HashMap<>();

    /** Maximum number of recent trades kept per instrument. */
    private int recentTradesCap = 50;

    // ------------------------------------------------------------------ //
    //  Startup                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Initialise all DAOs and load every cache.  Must be called once at startup
     * before any service is invoked.
     *
     * @param cap maximum recent trades per instrument to hold in memory
     */
    public void startup(int cap) {
        this.recentTradesCap = cap;

        instrumentDao       = new InstrumentDaoImpl();
        walletDao           = new WalletDaoImpl();
        holdingDao          = new HoldingDaoImpl();
        traderAssignmentDao = new TraderAssignmentDaoImpl();
        tradeDao            = new TradeDaoImpl();
        orderDao            = new OrderDaoImpl();

        loadInstruments();
        loadWallets();
        loadHoldings();
        loadTraderAssignments();
        loadRecentTrades();
        rebuildOrderBooks();
        recalculateReservations();
    }

    // ------------------------------------------------------------------ //
    //  Private loaders                                                    //
    // ------------------------------------------------------------------ //

    private void loadInstruments() {
        instrumentCache.clear();
        List<Instrument> instruments = instrumentDao.findAllActive();
        for (Instrument i : instruments) {
            instrumentCache.put(i.getSymbol(), i);
            orderBooks.put(i.getSymbol(), new OrderBook(i.getSymbol()));
        }
        System.out.println("[Cache] Loaded " + instrumentCache.size() + " active instruments.");
    }

    private void loadWallets() {
        walletCache.clear();
        List<Wallet> wallets = walletDao.findAll();
        for (Wallet w : wallets) {
            w.setReservedBalance(BigDecimal.ZERO);
            walletCache.put(w.getClientId(), w);
        }
        System.out.println("[Cache] Loaded " + walletCache.size() + " wallets.");
    }

    private void loadHoldings() {
        holdingCache.clear();
        List<Holding> holdings = holdingDao.findAll();
        for (Holding h : holdings) {
            h.setReservedQuantity(0L);
            holdingCache
                .computeIfAbsent(h.getClientId(), k -> new HashMap<>())
                .put(h.getInstrumentId(), h);
        }
        System.out.println("[Cache] Loaded holdings for " + holdingCache.size() + " clients.");
    }

    private void loadTraderAssignments() {
        traderAssignments.clear();
        List<TraderClientAssignment> assignments = traderAssignmentDao.findAllActive();
        for (TraderClientAssignment a : assignments) {
            traderAssignments
                .computeIfAbsent(a.getTraderId(), k -> new HashSet<>())
                .add(a.getClientId());
        }
        System.out.println("[Cache] Loaded " + assignments.size() + " trader-client assignments.");
    }

    private void loadRecentTrades() {
        recentTradesCache.clear();
        for (Instrument instrument : instrumentCache.values()) {
            List<Trade> recent = tradeDao.findRecentByInstrument(instrument.getInstrumentId(), recentTradesCap);
            ArrayList<Trade> capped = new ArrayList<>(recent);
            recentTradesCache.put(instrument.getSymbol(), capped);
        }
        System.out.println("[Cache] Loaded recent trades for " + recentTradesCache.size() + " instruments.");
    }

    private void rebuildOrderBooks() {
        orderNodeMap.clear();
        List<Order> openOrders = orderDao.findOpenOrders();
        for (Order order : openOrders) {
            OrderBook book = orderBooks.get(order.getSymbol());
            if (book == null) {
                // Instrument may have been delisted after the order was placed.
                continue;
            }
            if (order.getPrice() == null) {
                // MARKET orders should never rest in the book; skip defensively.
                continue;
            }
            OrderNode node = book.addOrder(order);
            orderNodeMap.put(order.getOrderId(), node);
        }
        System.out.println("[Cache] Rebuilt order books from " + openOrders.size() + " open orders.");
    }

    /**
     * Walk every resting order and lock up the corresponding wallet / holding
     * reservations so validation is accurate after a restart.
     */
    private void recalculateReservations() {
        for (OrderNode node : orderNodeMap.values()) {
            Order order = node.getOrder();
            if (order.getSide() == Side.BUY) {
                Wallet wallet = walletCache.get(order.getClientId());
                if (wallet != null && order.getPrice() != null) {
                    BigDecimal reservation = order.getPrice()
                        .multiply(BigDecimal.valueOf(order.getRemainingQty()));
                    wallet.setReservedBalance(wallet.getReservedBalance().add(reservation));
                }
            } else { // SELL
                HashMap<Integer, Holding> clientHoldings = holdingCache.get(order.getClientId());
                if (clientHoldings != null) {
                    Holding holding = clientHoldings.get(order.getInstrumentId());
                    if (holding != null) {
                        holding.setReservedQuantity(holding.getReservedQuantity() + order.getRemainingQty());
                    }
                }
            }
        }
        System.out.println("[Cache] Reservations recalculated from open orders.");
    }

    // ------------------------------------------------------------------ //
    //  Instrument cache                                                   //
    // ------------------------------------------------------------------ //

    public Instrument getInstrument(String symbol) {
        return instrumentCache.get(symbol);
    }

    public Collection<Instrument> getAllInstruments() {
        return Collections.unmodifiableCollection(instrumentCache.values());
    }

    public void updateInstrumentLastPrice(String symbol, BigDecimal price) {
        Instrument instrument = instrumentCache.get(symbol);
        if (instrument != null) {
            instrument.setLastTradedPrice(price);
        }
    }

    // ------------------------------------------------------------------ //
    //  Wallet cache                                                       //
    // ------------------------------------------------------------------ //

    public Wallet getWallet(long clientId) {
        return walletCache.get(clientId);
    }

    public Collection<Wallet> getAllWallets() {
        return Collections.unmodifiableCollection(walletCache.values());
    }

    // ------------------------------------------------------------------ //
    //  Holding cache                                                      //
    // ------------------------------------------------------------------ //

    public Holding getHolding(long clientId, int instrumentId) {
        HashMap<Integer, Holding> clientHoldings = holdingCache.get(clientId);
        if (clientHoldings == null) return null;
        return clientHoldings.get(instrumentId);
    }

    /**
     * Upsert a holding into the cache. Used by SettlementService after a
     * successful commit when a client acquires a new instrument for the first time.
     */
    public void putHolding(Holding holding) {
        holdingCache
            .computeIfAbsent(holding.getClientId(), k -> new HashMap<>())
            .put(holding.getInstrumentId(), holding);
    }

    public Map<Integer, Holding> getHoldingsForClient(long clientId) {
        HashMap<Integer, Holding> map = holdingCache.get(clientId);
        return map == null ? Collections.emptyMap() : Collections.unmodifiableMap(map);
    }

    // ------------------------------------------------------------------ //
    //  Trader → Client assignments                                        //
    // ------------------------------------------------------------------ //

    public boolean isTraderAssignedToClient(long traderId, long clientId) {
        HashSet<Long> clients = traderAssignments.get(traderId);
        return clients != null && clients.contains(clientId);
    }

    public List<Long> getClientsForTrader(long traderId) {
        HashSet<Long> clients = traderAssignments.get(traderId);
        return clients == null ? Collections.emptyList() : new ArrayList<>(clients);
    }

    // ------------------------------------------------------------------ //
    //  Recent trades cache                                                //
    // ------------------------------------------------------------------ //

    public List<Trade> getRecentTrades(String symbol) {
        ArrayList<Trade> list = recentTradesCache.get(symbol);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public void addRecentTrade(String symbol, Trade trade) {
        ArrayList<Trade> list = recentTradesCache.computeIfAbsent(symbol, k -> new ArrayList<>());
        list.add(0, trade);   // newest first
        if (list.size() > recentTradesCap) {
            list.remove(list.size() - 1);
        }
    }

    // ------------------------------------------------------------------ //
    //  Order book                                                         //
    // ------------------------------------------------------------------ //

    public OrderBook getOrderBook(String symbol) {
        return orderBooks.get(symbol);
    }

    // ------------------------------------------------------------------ //
    //  OrderNode map (O(1) cancel / modify)                              //
    // ------------------------------------------------------------------ //

    public OrderNode getOrderNode(long orderId) {
        return orderNodeMap.get(orderId);
    }

    public void putOrderNode(long orderId, OrderNode node) {
        orderNodeMap.put(orderId, node);
    }

    public void removeOrderNode(long orderId) {
        orderNodeMap.remove(orderId);
    }

    public boolean hasOpenOrder(long orderId) {
        return orderNodeMap.containsKey(orderId);
    }
}
