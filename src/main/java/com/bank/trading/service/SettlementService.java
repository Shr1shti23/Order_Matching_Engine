package com.bank.trading.service;

import com.bank.trading.cache.CacheManager;
import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.*;
import com.bank.trading.dao.impl.*;
import com.bank.trading.model.*;
import com.bank.trading.util.MatchResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists matched trades and all side-effects in a single database transaction.
 *
 * <h2>Transaction boundary</h2>
 * <p>One call to {@link #settle} opens exactly one JDBC transaction.  Either
 * everything commits or everything rolls back.  <strong>Caches are updated only
 * after a successful commit.</strong></p>
 *
 * <h2>What settlement does per fill</h2>
 * <ol>
 *   <li>Insert the {@code trades} row.</li>
 *   <li>Update both orders' {@code remaining_qty} and {@code status}.</li>
 *   <li>Debit buyer's wallet (TRADE_DEBIT) and credit seller's wallet (TRADE_CREDIT).</li>
 *   <li>Update buyer's holdings (upsert with weighted-average cost).</li>
 *   <li>Update seller's holdings (reduce quantity).</li>
 *   <li>Insert {@code wallet_transactions} for both sides.</li>
 *   <li>Insert {@code holding_transactions} for both sides.</li>
 *   <li>Insert {@code order_events} for both orders.</li>
 *   <li>Update {@code instruments.last_traded_price} (also done by DB trigger, but we
 *       keep the cache in sync here).</li>
 *   <li>Insert {@code audit_log} (TRADE_EXECUTED, also handled by DB trigger;
 *       we insert here to provide the actor context when available).</li>
 * </ol>
 */
public final class SettlementService {

    private final CacheManager         cache;
    private final ReservationService   reservationService;
    private final OrderBookService     orderBookService;

    private final OrderDao             orderDao;
    private final TradeDao             tradeDao;
    private final WalletDao            walletDao;
    private final HoldingDao           holdingDao;
    private final WalletTransactionDao walletTxDao;
    private final HoldingTransactionDao holdingTxDao;
    private final OrderEventDao        orderEventDao;
    private final AuditLogDao          auditLogDao;
    private final InstrumentDao        instrumentDao;

    public SettlementService(CacheManager cache,
                             ReservationService reservationService,
                             OrderBookService orderBookService) {
        this.cache              = cache;
        this.reservationService = reservationService;
        this.orderBookService   = orderBookService;

        this.orderDao      = new OrderDaoImpl();
        this.tradeDao      = new TradeDaoImpl();
        this.walletDao     = new WalletDaoImpl();
        this.holdingDao    = new HoldingDaoImpl();
        this.walletTxDao   = new WalletTransactionDaoImpl();
        this.holdingTxDao  = new HoldingTransactionDaoImpl();
        this.orderEventDao = new OrderEventDaoImpl();
        this.auditLogDao   = new AuditLogDaoImpl();
        this.instrumentDao = new InstrumentDaoImpl();
    }

    /**
     * Settle all fills produced by the matching engine.
     *
     * @param incoming  the incoming order (may be partially or fully filled)
     * @param matches   list of fills — must not be empty
     * @throws RuntimeException wrapping any {@link SQLException} on DB failure;
     *                          caches are NOT modified if an exception is thrown
     */
    public void settle(Order incoming, List<MatchResult> matches) {
        if (matches.isEmpty()) return;

        // Capture cache snapshots for rollback in case the commit fails.
        // (Caches are only mutated AFTER the commit block below.)

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<CacheUpdate> cacheUpdates = new ArrayList<>();

                for (MatchResult match : matches) {
                    CacheUpdate update = settleSingleFill(incoming, match, conn);
                    cacheUpdates.add(update);
                }

                conn.commit();

                // ----- DB committed: now update caches -----
                for (CacheUpdate u : cacheUpdates) {
                    applyCacheUpdate(u);
                }

            } catch (SQLException ex) {
                try { conn.rollback(); } catch (SQLException ignore) {}
                throw new RuntimeException("Settlement failed and was rolled back: " + ex.getMessage(), ex);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Cannot obtain DB connection for settlement: " + ex.getMessage(), ex);
        }
    }

    // ------------------------------------------------------------------ //
    //  Per-fill settlement                                                //
    // ------------------------------------------------------------------ //

    /**
     * Persist one fill to the database inside the supplied transaction.
     * Does NOT touch the cache.
     *
     * @return a {@link CacheUpdate} record describing every cache change to apply
     *         after the commit
     */
    private CacheUpdate settleSingleFill(Order incoming,
                                          MatchResult match,
                                          Connection conn) throws SQLException {

        // ----- Resolve the two order objects from cache -----
        Order buyOrder;
        Order sellOrder;
        if (incoming.getSide() == Side.BUY) {
            buyOrder  = incoming;
            sellOrder = cache.getOrderNode(match.getRestingOrderId()).getOrder();
        } else {
            sellOrder = incoming;
            buyOrder  = cache.getOrderNode(match.getRestingOrderId()).getOrder();
        }

        long   fillQty   = match.getQuantity();
        BigDecimal fillPrice = match.getExecutionPrice();   // resting order's price

        long buyClientId  = buyOrder.getClientId();
        long sellClientId = sellOrder.getClientId();
        int  instrumentId = incoming.getInstrumentId();
        String symbol     = incoming.getSymbol();

        // ----- 1. Insert trade -----
        Trade trade = new Trade();
        trade.setInstrumentId(instrumentId);
        trade.setBuyOrderId(buyOrder.getOrderId());
        trade.setSellOrderId(sellOrder.getOrderId());
        trade.setPrice(fillPrice);
        trade.setQuantity(fillQty);
        long tradeId = tradeDao.insert(trade, conn);
        trade.setTradeId(tradeId);

        // ----- 2. Determine new order states -----
        long buyNewRemaining  = buyOrder.getRemainingQty()  - fillQty;
        long sellNewRemaining = sellOrder.getRemainingQty() - fillQty;

        OrderStatus buyNewStatus  = deriveStatus(buyOrder,  buyNewRemaining);
        OrderStatus sellNewStatus = deriveStatus(sellOrder, sellNewRemaining);

        orderDao.updateStatus(buyOrder.getOrderId(),  buyNewStatus,  buyNewRemaining,  conn);
        orderDao.updateStatus(sellOrder.getOrderId(), sellNewStatus, sellNewRemaining, conn);

        // ----- 3. Wallet updates -----
        BigDecimal tradeCost = fillPrice.multiply(BigDecimal.valueOf(fillQty)).setScale(2, RoundingMode.HALF_UP);

        Wallet buyerWallet  = cache.getWallet(buyClientId);
        Wallet sellerWallet = cache.getWallet(sellClientId);

        BigDecimal buyerNewBalance  = buyerWallet.getCashBalance().subtract(tradeCost);
        BigDecimal sellerNewBalance = sellerWallet.getCashBalance().add(tradeCost);

        walletDao.updateBalance(buyerWallet.getWalletId(),  buyerNewBalance,  buyerWallet.getVersion(),  conn);
        walletDao.updateBalance(sellerWallet.getWalletId(), sellerNewBalance, sellerWallet.getVersion(), conn);

        // ----- 4. Wallet transactions -----
        WalletTransaction debit = new WalletTransaction();
        debit.setWalletId(buyerWallet.getWalletId());
        debit.setTransactionType(WalletTxType.TRADE_DEBIT);
        debit.setAmount(tradeCost.negate());
        debit.setBalanceAfter(buyerNewBalance);
        debit.setTradeId(tradeId);
        debit.setReference("Buy " + fillQty + " " + symbol + " @ " + fillPrice);
        walletTxDao.insert(debit, conn);

        WalletTransaction credit = new WalletTransaction();
        credit.setWalletId(sellerWallet.getWalletId());
        credit.setTransactionType(WalletTxType.TRADE_CREDIT);
        credit.setAmount(tradeCost);
        credit.setBalanceAfter(sellerNewBalance);
        credit.setTradeId(tradeId);
        credit.setReference("Sell " + fillQty + " " + symbol + " @ " + fillPrice);
        walletTxDao.insert(credit, conn);

        // ----- 5. Holding updates -----
        // Buyer acquires shares
        Holding buyerHolding = cache.getHolding(buyClientId, instrumentId);

        Holding buyDelta = new Holding();
        buyDelta.setClientId(buyClientId);
        buyDelta.setInstrumentId(instrumentId);
        buyDelta.setQuantity(fillQty);
        buyDelta.setAvgBuyPrice(fillPrice);
        holdingDao.upsert(buyDelta, conn);

        // Seller loses shares
        Holding sellerHolding = cache.getHolding(sellClientId, instrumentId);
        long sellerNewQty = (sellerHolding != null ? sellerHolding.getQuantity() : 0L) - fillQty;
        BigDecimal sellerAvgPrice = (sellerHolding != null ? sellerHolding.getAvgBuyPrice() : BigDecimal.ZERO);
        if (sellerHolding != null) {
            holdingDao.updateQuantity(sellerHolding.getHoldingId(), sellerNewQty, sellerAvgPrice, conn);
        }

        // ----- 6. Holding transactions -----
        HoldingTransaction buyHoldingTx = new HoldingTransaction();
        buyHoldingTx.setClientId(buyClientId);
        buyHoldingTx.setInstrumentId(instrumentId);
        buyHoldingTx.setTradeId(tradeId);
        buyHoldingTx.setTransactionType(HoldingTxType.BUY);
        buyHoldingTx.setQuantity(fillQty);
        buyHoldingTx.setPrice(fillPrice);
        holdingTxDao.insert(buyHoldingTx, conn);

        HoldingTransaction sellHoldingTx = new HoldingTransaction();
        sellHoldingTx.setClientId(sellClientId);
        sellHoldingTx.setInstrumentId(instrumentId);
        sellHoldingTx.setTradeId(tradeId);
        sellHoldingTx.setTransactionType(HoldingTxType.SELL);
        sellHoldingTx.setQuantity(-fillQty);
        sellHoldingTx.setPrice(fillPrice);
        holdingTxDao.insert(sellHoldingTx, conn);

        // ----- 7. Order events -----
        insertOrderEvent(buyOrder,  buyNewStatus,  buyNewRemaining,  fillQty, fillPrice, conn);
        insertOrderEvent(sellOrder, sellNewStatus, sellNewRemaining, fillQty, fillPrice, conn);

        // ----- 8. Update instruments.last_traded_price -----
        instrumentDao.updateLastTradedPrice(instrumentId, fillPrice, conn);

        // ----- 9. Audit log -----
        AuditLog audit = new AuditLog();
        audit.setActorUserId(null);  // system — trigger will also insert this
        audit.setActionType(ActionType.TRADE_EXECUTED);
        audit.setEntityType("trades");
        audit.setEntityId(tradeId);
        audit.setDetails("{\"instrument\":\"" + symbol + "\",\"price\":" + fillPrice
            + ",\"quantity\":" + fillQty + "}");
        auditLogDao.insert(audit, conn);

        // ----- Build the cache update descriptor -----
        return new CacheUpdate(
            trade,
            buyOrder,  buyNewRemaining,  buyNewStatus,
            sellOrder, sellNewRemaining, sellNewStatus,
            buyClientId,  buyerWallet.getWalletId(),  buyerNewBalance,  buyerWallet.getVersion() + 1,
            sellClientId, sellerWallet.getWalletId(), sellerNewBalance, sellerWallet.getVersion() + 1,
            buyerHolding, buyDelta, fillQty,
            sellerHolding, sellerNewQty, sellerAvgPrice,
            instrumentId, symbol, fillPrice
        );
    }

    // ------------------------------------------------------------------ //
    //  Cache application (called only after successful commit)           //
    // ------------------------------------------------------------------ //

    private void applyCacheUpdate(CacheUpdate u) {
        // Orders
        u.buyOrder.setRemainingQty(u.buyNewRemaining);
        u.buyOrder.setStatus(u.buyNewStatus);
        u.sellOrder.setRemainingQty(u.sellNewRemaining);
        u.sellOrder.setStatus(u.sellNewStatus);

        // Remove fully filled orders from book + nodeMap
        if (u.buyNewStatus == OrderStatus.FILLED) {
            orderBookService.removeOrderFromBook(u.buyOrder.getOrderId());
        }
        if (u.sellNewStatus == OrderStatus.FILLED) {
            orderBookService.removeOrderFromBook(u.sellOrder.getOrderId());
        }

        // Wallets
        Wallet buyerWallet  = cache.getWallet(u.buyClientId);
        Wallet sellerWallet = cache.getWallet(u.sellClientId);

        if (buyerWallet != null) {
            buyerWallet.setCashBalance(u.buyerNewBalance);
            buyerWallet.setVersion(u.buyerNewVersion);
            reservationService.reduceBuyReservation(u.buyClientId,
                u.trade.getPrice(), u.trade.getQuantity());
        }
        if (sellerWallet != null) {
            sellerWallet.setCashBalance(u.sellerNewBalance);
            sellerWallet.setVersion(u.sellerNewVersion);
            reservationService.reduceSellReservation(u.sellClientId,
                u.instrumentId, u.trade.getQuantity());
        }

        // Buyer holdings
        if (u.existingBuyerHolding == null) {
            // First time this client owns this instrument
            Holding newH = new Holding();
            newH.setClientId(u.buyClientId);
            newH.setInstrumentId(u.instrumentId);
            newH.setQuantity(u.buyDelta.getQuantity());
            newH.setAvgBuyPrice(u.buyDelta.getAvgBuyPrice());
            newH.setReservedQuantity(0L);
            cache.putHolding(newH);
        } else {
            // Weighted-average update (mirrors DB upsert logic)
            long oldQty = u.existingBuyerHolding.getQuantity();
            BigDecimal oldAvg = u.existingBuyerHolding.getAvgBuyPrice();
            long addQty = u.buyDelta.getQuantity();
            BigDecimal addPrice = u.buyDelta.getAvgBuyPrice();

            BigDecimal newAvg = (oldAvg.multiply(BigDecimal.valueOf(oldQty))
                .add(addPrice.multiply(BigDecimal.valueOf(addQty))))
                .divide(BigDecimal.valueOf(oldQty + addQty), 4, RoundingMode.HALF_UP);

            u.existingBuyerHolding.setQuantity(oldQty + addQty);
            u.existingBuyerHolding.setAvgBuyPrice(newAvg);
        }

        // Seller holdings
        if (u.existingSellerHolding != null) {
            u.existingSellerHolding.setQuantity(u.sellerNewQty);
        }

        // Instrument last price + recent trades cache
        cache.updateInstrumentLastPrice(u.symbol, u.fillPrice);
        cache.addRecentTrade(u.symbol, u.trade);
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                            //
    // ------------------------------------------------------------------ //

    private OrderStatus deriveStatus(Order order, long newRemaining) {
        if (newRemaining == 0) return OrderStatus.FILLED;
        if (newRemaining < order.getOriginalQty()) return OrderStatus.PARTIALLY_FILLED;
        return OrderStatus.PENDING;
    }

    private void insertOrderEvent(Order order, OrderStatus newStatus, long newRemaining,
                                  long fillQty, BigDecimal fillPrice,
                                  Connection conn) throws SQLException {
        OrderEvent event = new OrderEvent();
        event.setOrderId(order.getOrderId());
        event.setEventType(newStatus == OrderStatus.FILLED
            ? OrderEventType.FILLED : OrderEventType.PARTIALLY_FILLED);
        event.setPreviousStatus(order.getStatus());
        event.setNewStatus(newStatus);
        event.setQuantityChanged(fillQty);
        event.setPrice(fillPrice);
        orderEventDao.insert(event, conn);
    }

    // ------------------------------------------------------------------ //
    //  Internal cache-update descriptor (avoids mid-commit cache writes)//
    // ------------------------------------------------------------------ //

    private static final class CacheUpdate {
        final Trade       trade;
        final Order       buyOrder;
        final long        buyNewRemaining;
        final OrderStatus buyNewStatus;
        final Order       sellOrder;
        final long        sellNewRemaining;
        final OrderStatus sellNewStatus;
        final long        buyClientId;
        final long        buyerWalletId;
        final BigDecimal  buyerNewBalance;
        final int         buyerNewVersion;
        final long        sellClientId;
        final long        sellerWalletId;
        final BigDecimal  sellerNewBalance;
        final int         sellerNewVersion;
        final Holding     existingBuyerHolding;   // may be null if first purchase
        final Holding     buyDelta;
        final long        buyFillQty;
        final Holding     existingSellerHolding;
        final long        sellerNewQty;
        final BigDecimal  sellerAvgPrice;
        final int         instrumentId;
        final String      symbol;
        final BigDecimal  fillPrice;

        CacheUpdate(Trade trade,
                    Order buyOrder, long buyNewRemaining, OrderStatus buyNewStatus,
                    Order sellOrder, long sellNewRemaining, OrderStatus sellNewStatus,
                    long buyClientId, long buyerWalletId, BigDecimal buyerNewBalance, int buyerNewVersion,
                    long sellClientId, long sellerWalletId, BigDecimal sellerNewBalance, int sellerNewVersion,
                    Holding existingBuyerHolding, Holding buyDelta, long buyFillQty,
                    Holding existingSellerHolding, long sellerNewQty, BigDecimal sellerAvgPrice,
                    int instrumentId, String symbol, BigDecimal fillPrice) {
            this.trade = trade;
            this.buyOrder = buyOrder;
            this.buyNewRemaining = buyNewRemaining;
            this.buyNewStatus = buyNewStatus;
            this.sellOrder = sellOrder;
            this.sellNewRemaining = sellNewRemaining;
            this.sellNewStatus = sellNewStatus;
            this.buyClientId = buyClientId;
            this.buyerWalletId = buyerWalletId;
            this.buyerNewBalance = buyerNewBalance;
            this.buyerNewVersion = buyerNewVersion;
            this.sellClientId = sellClientId;
            this.sellerWalletId = sellerWalletId;
            this.sellerNewBalance = sellerNewBalance;
            this.sellerNewVersion = sellerNewVersion;
            this.existingBuyerHolding = existingBuyerHolding;
            this.buyDelta = buyDelta;
            this.buyFillQty = buyFillQty;
            this.existingSellerHolding = existingSellerHolding;
            this.sellerNewQty = sellerNewQty;
            this.sellerAvgPrice = sellerAvgPrice;
            this.instrumentId = instrumentId;
            this.symbol = symbol;
            this.fillPrice = fillPrice;
        }
    }
}
