package com.bank.trading.service;

import com.bank.trading.engine.OrderBook;
import com.bank.trading.engine.OrderNode;
import com.bank.trading.engine.PriceLevel;
import com.bank.trading.model.*;
import com.bank.trading.util.MatchResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure, read-only matching engine.
 *
 * <h2>Guarantees</h2>
 * <ul>
 *   <li>Single-threaded — no locks, no concurrent structures.</li>
 *   <li><strong>Never</strong> writes to the database.</li>
 *   <li><strong>Never</strong> mutates the order book or any cache.</li>
 *   <li>Execution price = the <em>resting</em> order's limit price (price-time
 *       priority; time = FIFO within each price level).</li>
 * </ul>
 *
 * <h2>Matching loop</h2>
 * <ol>
 *   <li>Select the opposite-side book.</li>
 *   <li>Get the best price level (lowest ask for BUY; highest bid for SELL).</li>
 *   <li>Check if the price is executable.</li>
 *   <li>Traverse FIFO through the level's nodes.</li>
 *   <li>Collect {@link MatchResult} records — no state is mutated here.</li>
 *   <li>Move to the next price level if quantity remains.</li>
 *   <li>Stop when remaining quantity reaches zero or no executable price exists.</li>
 * </ol>
 *
 * <p>Settlement and order-book updates are performed by {@link SettlementService}
 * after a successful database commit.</p>
 */
public final class MatchingEngine {

    /**
     * Simulate matching for an incoming order against the live order book.
     *
     * <p>The method is entirely read-only: it tracks remaining quantity locally
     * and returns a list of {@link MatchResult} that describes every fill pair.
     * The caller ({@link SettlementService}) applies those results to the DB and
     * then to the cache.</p>
     *
     * <p>For <strong>FOK</strong> orders, use {@link #canFullyFill(Order, OrderBook)}
     * first; if that returns {@code false}, do not call this method.</p>
     *
     * @param incoming the new order (remainingQty must equal originalQty at entry)
     * @param book     the order book for the incoming order's instrument
     * @return ordered list of match results (may be empty if no liquidity)
     */
    public List<MatchResult> match(Order incoming, OrderBook book) {
        List<MatchResult> results = new ArrayList<>();
        if (incoming.getSide() == Side.BUY) {
            matchBuyOrder(incoming, book, results);
        } else {
            matchSellOrder(incoming, book, results);
        }
        return results;
    }

    /**
     * Dry-run: check whether a FOK order can be fully filled without changing
     * any state.
     */
    public boolean canFullyFill(Order incoming, OrderBook book) {
        List<MatchResult> dryRun = match(incoming, book);
        long filled = dryRun.stream().mapToLong(MatchResult::getQuantity).sum();
        return filled >= incoming.getRemainingQty();
    }

    // ------------------------------------------------------------------ //
    //  Side-specific matching loops                                       //
    // ------------------------------------------------------------------ //

    private void matchBuyOrder(Order buy, OrderBook book, List<MatchResult> results) {
        long localRemaining = buy.getRemainingQty();

        for (Map.Entry<BigDecimal, PriceLevel> entry : book.getSellBook().entrySet()) {
            if (localRemaining == 0) break;

            BigDecimal levelPrice = entry.getKey();

            // Price check: LIMIT buy must offer >= ask; MARKET always matches.
            if (buy.getOrderType() == OrderType.LIMIT
                    && buy.getPrice().compareTo(levelPrice) < 0) {
                break;  // No more executable prices (sell book is ascending).
            }

            localRemaining = matchAtLevel(buy, entry.getValue(), levelPrice, localRemaining, results);
        }
    }

    private void matchSellOrder(Order sell, OrderBook book, List<MatchResult> results) {
        long localRemaining = sell.getRemainingQty();

        for (Map.Entry<BigDecimal, PriceLevel> entry : book.getBuyBook().entrySet()) {
            if (localRemaining == 0) break;

            BigDecimal levelPrice = entry.getKey();

            // Price check: LIMIT sell must ask <= bid; MARKET always matches.
            if (sell.getOrderType() == OrderType.LIMIT
                    && sell.getPrice().compareTo(levelPrice) > 0) {
                break;  // No more executable prices (buy book is descending).
            }

            localRemaining = matchAtLevel(sell, entry.getValue(), levelPrice, localRemaining, results);
        }
    }

    /**
     * Traverse one price level FIFO, collecting fills.
     *
     * @return updated localRemaining after consuming orders at this level
     */
    private long matchAtLevel(Order incoming,
                               PriceLevel level,
                               BigDecimal executionPrice,
                               long localRemaining,
                               List<MatchResult> results) {
        // Track how much of each resting order is still available within this
        // match() call.  We cannot read resting.getRemainingQty() as-is for
        // orders that appeared in an earlier price level in this same call,
        // but since we traverse level by level and orders sit at exactly one
        // price level, the in-cache remainingQty is always current.
        OrderNode node = level.getHead();

        while (node != null && localRemaining > 0) {
            Order resting = node.getOrder();

            // Self-trade prevention: skip if same client on both sides.
            if (resting.getClientId() == incoming.getClientId()) {
                node = node.getNext();
                continue;
            }

            long fillQty = Math.min(localRemaining, resting.getRemainingQty());

            long buyOrderId  = (incoming.getSide() == Side.BUY)
                ? incoming.getOrderId() : resting.getOrderId();
            long sellOrderId = (incoming.getSide() == Side.SELL)
                ? incoming.getOrderId() : resting.getOrderId();

            results.add(new MatchResult(buyOrderId, sellOrderId, executionPrice,
                                        fillQty, resting.getOrderId()));

            localRemaining -= fillQty;
            node = node.getNext();
        }

        return localRemaining;
    }
}
