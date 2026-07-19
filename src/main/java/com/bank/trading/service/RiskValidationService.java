package com.bank.trading.service;

import com.bank.trading.cache.CacheManager;
import com.bank.trading.model.*;
import com.bank.trading.util.ValidationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Validates an order before it touches the order book.
 *
 * <p>All checks run purely against the in-memory cache.  No database reads
 * are performed here.</p>
 */
public final class RiskValidationService {

    private final CacheManager cache;

    public RiskValidationService(CacheManager cache) {
        this.cache = cache;
    }

    /**
     * Full pre-trade validation.
     *
     * @param order    the incoming order (orderId may be 0 — not yet persisted)
     * @param traderId the trader placing this order
     * @return {@link ValidationResult#ok()} or {@link ValidationResult#fail(String)}
     */
    public ValidationResult validate(Order order, long traderId) {

        // 1. Trader must be assigned to the client
        if (!cache.isTraderAssignedToClient(traderId, order.getClientId())) {
            return ValidationResult.fail(
                "Trader " + traderId + " is not assigned to client " + order.getClientId());
        }

        // 2. Instrument must be active
        Instrument instrument = cache.getInstrument(order.getSymbol());
        if (instrument == null) {
            return ValidationResult.fail("Instrument not found: " + order.getSymbol());
        }
        if (instrument.getStatus() != InstrumentStatus.ACTIVE) {
            return ValidationResult.fail(
                "Instrument " + order.getSymbol() + " is not active (status=" + instrument.getStatus() + ")");
        }

        // 3. Quantity must be positive and a multiple of lot size
        if (order.getOriginalQty() <= 0) {
            return ValidationResult.fail("Order quantity must be positive, got " + order.getOriginalQty());
        }
        if (order.getOriginalQty() % instrument.getLotSize() != 0) {
            return ValidationResult.fail(
                "Quantity " + order.getOriginalQty() + " is not a multiple of lot size " + instrument.getLotSize());
        }

        // 4. LIMIT orders: price must be present and valid
        if (order.getOrderType() == OrderType.LIMIT) {
            if (order.getPrice() == null) {
                return ValidationResult.fail("LIMIT orders must have a price.");
            }
            if (order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return ValidationResult.fail("LIMIT order price must be positive.");
            }
            // Tick size check
            ValidationResult tickResult = validateTickSize(order.getPrice(), instrument.getTickSize());
            if (!tickResult.isValid()) {
                return tickResult;
            }
        }

        // 5. BUY orders: sufficient wallet balance
        if (order.getSide() == Side.BUY) {
            return validateBuyFunds(order, instrument);
        }

        // 6. SELL orders: sufficient holdings
        if (order.getSide() == Side.SELL) {
            return validateSellHoldings(order, instrument);
        }

        return ValidationResult.ok();
    }

    // ------------------------------------------------------------------ //
    //  Private helpers                                                    //
    // ------------------------------------------------------------------ //

    private ValidationResult validateTickSize(BigDecimal price, BigDecimal tickSize) {
        if (tickSize.compareTo(BigDecimal.ZERO) == 0) {
            return ValidationResult.ok();
        }
        BigDecimal[] divResult = price.divideAndRemainder(tickSize);
        if (divResult[1].compareTo(BigDecimal.ZERO) != 0) {
            return ValidationResult.fail(
                "Price " + price + " is not a valid tick size multiple of " + tickSize);
        }
        return ValidationResult.ok();
    }

    private ValidationResult validateBuyFunds(Order order, Instrument instrument) {
        Wallet wallet = cache.getWallet(order.getClientId());
        if (wallet == null) {
            return ValidationResult.fail("No wallet found for client " + order.getClientId());
        }
        if (order.getOrderType() == OrderType.MARKET) {
            // MARKET BUY — we cannot know the exact cost yet.
            // At minimum the wallet must be non-zero.
            if (wallet.getAvailableBalance().compareTo(BigDecimal.ZERO) <= 0) {
                return ValidationResult.fail("Insufficient wallet balance for MARKET BUY order.");
            }
            return ValidationResult.ok();
        }
        // LIMIT BUY — worst-case cost = price × qty
        BigDecimal required = order.getPrice()
            .multiply(BigDecimal.valueOf(order.getOriginalQty()))
            .setScale(2, RoundingMode.HALF_UP);
        if (wallet.getAvailableBalance().compareTo(required) < 0) {
            return ValidationResult.fail(
                "Insufficient wallet balance. Required: " + required
                + ", Available: " + wallet.getAvailableBalance());
        }
        return ValidationResult.ok();
    }

    private ValidationResult validateSellHoldings(Order order, Instrument instrument) {
        Holding holding = cache.getHolding(order.getClientId(), instrument.getInstrumentId());
        if (holding == null || holding.getAvailableQuantity() < order.getOriginalQty()) {
            long available = (holding == null) ? 0 : holding.getAvailableQuantity();
            return ValidationResult.fail(
                "Insufficient holdings. Required: " + order.getOriginalQty()
                + ", Available: " + available);
        }
        return ValidationResult.ok();
    }
}
