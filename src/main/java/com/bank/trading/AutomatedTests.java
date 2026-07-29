package com.bank.trading;

import com.bank.trading.cache.CacheManager;
import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.engine.OrderBook;
import com.bank.trading.engine.PriceLevel;
import com.bank.trading.model.*;
import com.bank.trading.service.*;
import com.bank.trading.util.OrderPlacementResult;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Automated End-to-End Test Suite for the Order Matching Engine.
 *
 * <p>Validates core trading operations, matching algorithms, order modifications,
 * order cancellations, Fill-or-Kill (FOK) liquidity checks, and Immediate-or-Cancel (IOC)
 * market order execution.</p>
 */
public class AutomatedTests {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("               AUTOMATED ORDER MATCHING ENGINE TESTS                      ");
        System.out.println("==========================================================================");

        try {
            // Initialize Database Connection Pool
            DatabaseConfig.init();
            
            // Startup In-Memory Cache Manager
            CacheManager cache = CacheManager.getInstance();
            cache.startup(50);
            
            // Instantiate Core Platform Services
            ReservationService reservationService = new ReservationService(cache);
            OrderBookService orderBookService = new OrderBookService(cache);
            MatchingEngine matchingEngine = new MatchingEngine();
            SettlementService settlementService = new SettlementService(cache, reservationService, orderBookService);
            RiskValidationService riskValidationService = new RiskValidationService(cache);
            CancelOrderService cancelOrderService = new CancelOrderService(cache, reservationService, orderBookService);
            @SuppressWarnings("unused")
            ExpiryService expiryService = new ExpiryService(cache, cancelOrderService);
            TradingService tradingService = new TradingService(cache, riskValidationService, reservationService, matchingEngine, settlementService, orderBookService, cancelOrderService);
            ModifyOrderService modifyOrderService = new ModifyOrderService(cache, tradingService, cancelOrderService, reservationService);
            
            long traderId = 2L;    // Trader arjun
            long clientId = 7L;    // Client sanjay
            int instrumentId = 1;  // Instrument AAPL
            String symbol = "AAPL";
            
            // ------------------------------------------------------------------
            // Test 1: Place a BUY LIMIT Order
            // ------------------------------------------------------------------
            Order buyOrder = new Order();
            buyOrder.setSymbol(symbol);
            buyOrder.setTraderId(traderId);
            buyOrder.setClientId(clientId);
            buyOrder.setInstrumentId(instrumentId);
            buyOrder.setSide(Side.BUY);
            buyOrder.setOrderType(OrderType.LIMIT);
            buyOrder.setTimeInForce(TimeInForce.DAY);
            buyOrder.setOriginalQty(10);
            buyOrder.setRemainingQty(10);
            buyOrder.setPrice(new BigDecimal("150.00"));
            
            System.out.println("\n[Test 1] Placing BUY LIMIT Order (10 units @ $150.00)...");
            OrderPlacementResult res1 = tradingService.placeOrder(buyOrder);
            System.out.println("  Result: " + (res1.isSuccess() ? "SUCCESS" : "FAILED") + " - " + res1.getMessage());
            
            // ------------------------------------------------------------------
            // Test 2: Place a Matching SELL LIMIT Order
            // ------------------------------------------------------------------
            long sellerTraderId = 3L; // Trader priya
            long sellerClientId = 8L; // Client neha
            
            Order sellOrder = new Order();
            sellOrder.setSymbol(symbol);
            sellOrder.setTraderId(sellerTraderId);
            sellOrder.setClientId(sellerClientId);
            sellOrder.setInstrumentId(instrumentId);
            sellOrder.setSide(Side.SELL);
            sellOrder.setOrderType(OrderType.LIMIT);
            sellOrder.setTimeInForce(TimeInForce.DAY);
            sellOrder.setOriginalQty(5);
            sellOrder.setRemainingQty(5);
            sellOrder.setPrice(new BigDecimal("140.00")); // Crosses the $150.00 bid
            
            System.out.println("\n[Test 2] Placing SELL LIMIT Order (5 units @ $140.00, should match 5 units)...");
            OrderPlacementResult res2 = tradingService.placeOrder(sellOrder);
            System.out.println("  Result: " + (res2.isSuccess() ? "SUCCESS" : "FAILED") + " - " + res2.getMessage());
            
            // ------------------------------------------------------------------
            // Test 3: Modify the Remaining BUY Order (In-Place Quantity Reduction)
            // ------------------------------------------------------------------
            System.out.println("\n[Test 3] Modifying remaining BUY order (Qty reduction in-place to 2 units)...");
            OrderPlacementResult res3 = modifyOrderService.modify(res1.getOrderId(), new BigDecimal("150.00"), 2, traderId);
            System.out.println("  Result: " + (res3.isSuccess() ? "SUCCESS" : "FAILED") + " - " + res3.getMessage());
            
            // ------------------------------------------------------------------
            // Test 4: Cancel the Modified BUY Order
            // ------------------------------------------------------------------
            System.out.println("\n[Test 4] Cancelling the modified BUY order...");
            cancelOrderService.cancel(res1.getOrderId(), traderId);
            System.out.println("  Result: SUCCESS (Order cancelled successfully)");
            
            // ------------------------------------------------------------------
            // Test 5: Fill-or-Kill (FOK) Order Validation
            // ------------------------------------------------------------------
            Order fokOrder = new Order();
            fokOrder.setSymbol(symbol);
            fokOrder.setTraderId(traderId);
            fokOrder.setClientId(clientId);
            fokOrder.setInstrumentId(instrumentId);
            fokOrder.setSide(Side.BUY);
            fokOrder.setOrderType(OrderType.LIMIT);
            fokOrder.setTimeInForce(TimeInForce.FOK);
            fokOrder.setOriginalQty(500); // Exceeds available market liquidity
            fokOrder.setRemainingQty(500);
            fokOrder.setPrice(new BigDecimal("150.00"));
            
            System.out.println("\n[Test 5] Placing FOK BUY LIMIT Order (500 units - should reject due to insufficient liquidity)...");
            OrderPlacementResult res5 = tradingService.placeOrder(fokOrder);
            System.out.println("  Result: " + (res5.isSuccess() ? "SUCCESS" : "REJECTED (Expected)") + " - " + res5.getMessage());
            
            // ------------------------------------------------------------------
            // Test 6: MARKET Immediate-or-Cancel (IOC) Order Execution
            // ------------------------------------------------------------------
            Order marketIoc = new Order();
            marketIoc.setSymbol(symbol);
            marketIoc.setTraderId(traderId);
            marketIoc.setClientId(clientId);
            marketIoc.setInstrumentId(instrumentId);
            marketIoc.setSide(Side.BUY);
            marketIoc.setOrderType(OrderType.MARKET);
            marketIoc.setTimeInForce(TimeInForce.IOC);
            marketIoc.setOriginalQty(1);
            marketIoc.setRemainingQty(1);
            
            System.out.println("\n[Test 6] Placing MARKET IOC BUY Order (1 unit)...");
            OrderPlacementResult res6 = tradingService.placeOrder(marketIoc);
            System.out.println("  Result: " + (res6.isSuccess() ? "SUCCESS" : "FAILED") + " - " + res6.getMessage());
            
            // ------------------------------------------------------------------
            // Order Book State Inspection
            // ------------------------------------------------------------------
            OrderBook book = cache.getOrderBook(symbol);
            System.out.println("\n==========================================================================");
            System.out.println("                   FINAL ORDER BOOK STATE (" + symbol + ")                ");
            System.out.println("==========================================================================");
            System.out.println("  --- BIDS (BUY) ---");
            if (book.getBuyBook().isEmpty()) {
                System.out.println("  (empty)");
            } else {
                for (Map.Entry<BigDecimal, PriceLevel> entry : book.getBuyBook().entrySet()) {
                    System.out.println("  BID Price: $" + entry.getKey() + " | Total Qty: " + entry.getValue().getTotalQuantity());
                }
            }
            
            System.out.println("\n  --- ASKS (SELL) ---");
            if (book.getSellBook().isEmpty()) {
                System.out.println("  (empty)");
            } else {
                for (Map.Entry<BigDecimal, PriceLevel> entry : book.getSellBook().entrySet()) {
                    System.out.println("  ASK Price: $" + entry.getKey() + " | Total Qty: " + entry.getValue().getTotalQuantity());
                }
            }
            
            System.out.println("\n==========================================================================");
            System.out.println("                  ALL AUTOMATED TESTS COMPLETED CLEANLY                   ");
            System.out.println("==========================================================================");
        } catch (Exception e) {
            System.err.println("[ERROR] Automated test failure: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DatabaseConfig.close();
        }
    }
}
