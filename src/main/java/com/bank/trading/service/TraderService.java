package com.bank.trading.service;

import com.bank.trading.cache.CacheManager;
import com.bank.trading.dao.OrderDao;
import com.bank.trading.dao.TradeDao;
import com.bank.trading.dao.impl.OrderDaoImpl;
import com.bank.trading.dao.impl.TradeDaoImpl;
import com.bank.trading.model.*;
import com.bank.trading.util.OrderPlacementResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Provides trader-scoped operations.
 * Enforces trader-client assignment and delegates all order operations
 * to existing services without modification.
 */
public final class TraderService {

    private final CacheManager        cache;
    private final TradingService      tradingService;
    private final CancelOrderService  cancelOrderService;
    private final ModifyOrderService  modifyOrderService;
    private final OrderDao            orderDao;
    private final TradeDao            tradeDao;

    public TraderService(CacheManager cache,
                         TradingService tradingService,
                         CancelOrderService cancelOrderService,
                         ModifyOrderService modifyOrderService) {
        this.cache              = cache;
        this.tradingService     = tradingService;
        this.cancelOrderService = cancelOrderService;
        this.modifyOrderService = modifyOrderService;
        this.orderDao           = new OrderDaoImpl();
        this.tradeDao           = new TradeDaoImpl();
    }

    // ================================================================== //
    //  Assignment guard                                                   //
    // ================================================================== //

    public void assertAssigned(long traderId, long clientId) {
        if (!cache.isTraderAssignedToClient(traderId, clientId)) {
            throw new IllegalArgumentException(
                "Trader " + traderId + " is not assigned to client " + clientId);
        }
    }

    // ================================================================== //
    //  Client views                                                       //
    // ================================================================== //

    public List<Long> getAssignedClients(long traderId) {
        return cache.getClientsForTrader(traderId);
    }

    public Map<Integer, Holding> getClientHoldings(long traderId, long clientId) {
        assertAssigned(traderId, clientId);
        return cache.getHoldingsForClient(clientId);
    }

    public Wallet getClientWallet(long traderId, long clientId) {
        assertAssigned(traderId, clientId);
        return cache.getWallet(clientId);
    }

    public List<Order> getClientOrders(long traderId, long clientId) {
        assertAssigned(traderId, clientId);
        return orderDao.findByClientId(clientId);
    }

    public List<Trade> getClientTrades(long traderId, long clientId) {
        assertAssigned(traderId, clientId);
        return tradeDao.findByClientId(clientId);
    }

    // ================================================================== //
    //  Order operations (delegates to existing services unchanged)        //
    // ================================================================== //

    public OrderPlacementResult placeOrder(long traderId, Order order) {
        assertAssigned(traderId, order.getClientId());
        return tradingService.placeOrder(order);
    }

    public void cancelOrder(long orderId, long actorUserId) {
        cancelOrderService.cancel(orderId, actorUserId);
    }

    public OrderPlacementResult modifyOrder(long orderId, BigDecimal newPrice,
                                            long newQty, long actorUserId) {
        return modifyOrderService.modify(orderId, newPrice, newQty, actorUserId);
    }
}
