package com.bank.trading.service;

import com.bank.trading.cache.CacheManager;
import com.bank.trading.engine.OrderBook;
import com.bank.trading.engine.OrderNode;
import com.bank.trading.engine.PriceLevel;
import com.bank.trading.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Expires DAY orders at the end of a trading session.
 *
 * <p>Iterates every live order in the order book for a given symbol,
 * selects those whose {@link TimeInForce} is {@link TimeInForce#DAY},
 * and delegates to {@link CancelOrderService#expire(Order)} for each one.</p>
 *
 * <p>PARTIALLY_FILLED orders are expired the same way as PENDING ones
 * per the design specification.</p>
 *
 * <p>Single-threaded — no locks required.</p>
 */
public final class ExpiryService {

    private final CacheManager       cache;
    private final CancelOrderService cancelOrderService;

    public ExpiryService(CacheManager cache, CancelOrderService cancelOrderService) {
        this.cache              = cache;
        this.cancelOrderService = cancelOrderService;
    }

    /**
     * Expire all DAY orders for a specific instrument symbol.
     *
     * @param symbol the instrument symbol whose DAY orders should be expired
     * @return the number of orders expired
     */
    public int expireBySymbol(String symbol) {
        OrderBook book = cache.getOrderBook(symbol);
        if (book == null) {
            return 0;
        }
        List<Order> toExpire = collectDayOrders(book);
        for (Order order : toExpire) {
            cancelOrderService.expire(order);
        }
        return toExpire.size();
    }

    /**
     * Expire all DAY orders across every active instrument.
     *
     * @return total number of orders expired
     */
    public int expireAll() {
        int total = 0;
        for (Instrument instrument : cache.getAllInstruments()) {
            total += expireBySymbol(instrument.getSymbol());
        }
        if (total > 0) {
            System.out.println("[Expiry] Expired " + total + " DAY order(s) across all instruments.");
        }
        return total;
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Collect all DAY orders from both sides of the book into a list so that
     * we can safely remove them without invalidating the TreeMap iterators.
     */
    private List<Order> collectDayOrders(OrderBook book) {
        List<Order> result = new ArrayList<>();
        collectFromSide(book.getBuyBook(),  result);
        collectFromSide(book.getSellBook(), result);
        return result;
    }

    private void collectFromSide(Map<BigDecimal, PriceLevel> side, List<Order> result) {
        for (PriceLevel level : side.values()) {
            OrderNode node = level.getHead();
            while (node != null) {
                Order order = node.getOrder();
                if (order.getTimeInForce() == TimeInForce.DAY) {
                    result.add(order);
                }
                node = node.getNext();
            }
        }
    }
}
