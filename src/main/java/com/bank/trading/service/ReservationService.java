package com.bank.trading.service;

import com.bank.trading.cache.CacheManager;
import com.bank.trading.model.*;

import java.math.BigDecimal;

/**
 * Manages in-memory fund and holding reservations.
 *
 * <p>Reservations are in-memory only.  The database {@code wallets.cash_balance}
 * column always holds the total (available + reserved).  The DB is only updated
 * during settlement after a successful trade.</p>
 *
 * <p>All methods are O(1) cache operations — no database access.</p>
 */
public final class ReservationService {

    private final CacheManager cache;

    public ReservationService(CacheManager cache) {
        this.cache = cache;
    }

    // ------------------------------------------------------------------ //
    //  BUY reservations                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Reserve funds for a BUY LIMIT order.
     * Moves {@code price × quantity} from available into reserved balance.
     */
    public void reserveBuyLimit(Order order) {
        Wallet wallet = cache.getWallet(order.getClientId());
        if (wallet == null) {
            throw new IllegalStateException("Wallet not found for client " + order.getClientId());
        }
        BigDecimal amount = order.getPrice()
            .multiply(BigDecimal.valueOf(order.getRemainingQty()));
        wallet.setReservedBalance(wallet.getReservedBalance().add(amount));
    }

    /**
     * Reserve funds for a BUY MARKET order.
     * Reserves the entire available balance as a ceiling —
     * the actual debit is determined during settlement.
     */
    public void reserveBuyMarket(Order order) {
        Wallet wallet = cache.getWallet(order.getClientId());
        if (wallet == null) {
            throw new IllegalStateException("Wallet not found for client " + order.getClientId());
        }
        // Reserve whatever is available — market orders fill at resting prices.
        BigDecimal available = wallet.getAvailableBalance();
        wallet.setReservedBalance(wallet.getReservedBalance().add(available));
    }

    /**
     * Release the buy reservation when an order is cancelled, rejected, or expired.
     * Quantity-aware: only releases {@code price × remainingQty} (not the original full amount).
     */
    public void releaseBuyReservation(Order order) {
        Wallet wallet = cache.getWallet(order.getClientId());
        if (wallet == null) return;

        if (order.getOrderType() == OrderType.MARKET) {
            // We reserved the full available balance; release all of it on cancel.
            // Any fills have already been settled and deducted from cashBalance,
            // so the remaining reservedBalance equals the unfilled portion.
            wallet.setReservedBalance(BigDecimal.ZERO
                .max(wallet.getReservedBalance().subtract(
                    order.getPrice() != null
                        ? order.getPrice().multiply(BigDecimal.valueOf(order.getRemainingQty()))
                        : wallet.getReservedBalance())));
        } else {
            // LIMIT: price × remainingQty
            BigDecimal release = order.getPrice()
                .multiply(BigDecimal.valueOf(order.getRemainingQty()));
            BigDecimal newReserved = wallet.getReservedBalance().subtract(release);
            wallet.setReservedBalance(newReserved.max(BigDecimal.ZERO));
        }
    }

    /**
     * Reduce the buy reservation after a partial or full fill.
     * Called by {@link SettlementService} for each trade, before the full commit.
     */
    public void reduceBuyReservation(long clientId, BigDecimal filledPrice, long filledQty) {
        Wallet wallet = cache.getWallet(clientId);
        if (wallet == null) return;
        BigDecimal reduction = filledPrice.multiply(BigDecimal.valueOf(filledQty));
        BigDecimal newReserved = wallet.getReservedBalance().subtract(reduction);
        wallet.setReservedBalance(newReserved.max(BigDecimal.ZERO));
    }

    // ------------------------------------------------------------------ //
    //  SELL reservations                                                  //
    // ------------------------------------------------------------------ //

    /**
     * Reserve holdings for a SELL order.
     * Moves {@code quantity} from available into reserved holdings.
     */
    public void reserveSell(Order order) {
        Holding holding = cache.getHolding(order.getClientId(), order.getInstrumentId());
        if (holding == null) {
            throw new IllegalStateException(
                "Holding not found for client " + order.getClientId()
                + " instrument " + order.getInstrumentId());
        }
        holding.setReservedQuantity(holding.getReservedQuantity() + order.getRemainingQty());
    }

    /**
     * Release holding reservation when a SELL order is cancelled, rejected, or expired.
     */
    public void releaseSellReservation(Order order) {
        Holding holding = cache.getHolding(order.getClientId(), order.getInstrumentId());
        if (holding == null) return;
        long newReserved = holding.getReservedQuantity() - order.getRemainingQty();
        holding.setReservedQuantity(Math.max(0L, newReserved));
    }

    /**
     * Reduce the sell reservation after a partial or full fill.
     * Called by {@link SettlementService} for each trade.
     */
    public void reduceSellReservation(long clientId, int instrumentId, long filledQty) {
        Holding holding = cache.getHolding(clientId, instrumentId);
        if (holding == null) return;
        long newReserved = holding.getReservedQuantity() - filledQty;
        holding.setReservedQuantity(Math.max(0L, newReserved));
    }

    // ------------------------------------------------------------------ //
    //  Unified entry points (called by TradingService)                   //
    // ------------------------------------------------------------------ //

    /** Reserve based on order side and type. */
    public void reserve(Order order) {
        if (order.getSide() == Side.BUY) {
            if (order.getOrderType() == OrderType.MARKET) {
                reserveBuyMarket(order);
            } else {
                reserveBuyLimit(order);
            }
        } else {
            reserveSell(order);
        }
    }

    /** Release the full remaining reservation on cancel / reject / expiry. */
    public void release(Order order) {
        if (order.getSide() == Side.BUY) {
            releaseBuyReservation(order);
        } else {
            releaseSellReservation(order);
        }
    }
}
