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

public class AutomatedTests {

    public static void main(String[] args) {
        System.out.println("Starting Automated Tests...");

        try {
            DatabaseConfig.init();
            
            CacheManager cache = CacheManager.getInstance();
            cache.startup(50);
            
            ReservationService reservationService = new ReservationService(cache);
            OrderBookService orderBookService = new OrderBookService(cache);
            MatchingEngine matchingEngine = new MatchingEngine();
            SettlementService settlementService = new SettlementService(cache, reservationService, orderBookService);
            RiskValidationService riskValidationService = new RiskValidationService(cache);
            CancelOrderService cancelOrderService = new CancelOrderService(cache, reservationService, orderBookService);
            ExpiryService expiryService = new ExpiryService(cache, cancelOrderService);
            TradingService tradingService = new TradingService(cache, riskValidationService, reservationService, matchingEngine, settlementService, orderBookService, cancelOrderService);
            ModifyOrderService modifyOrderService = new ModifyOrderService(cache, tradingService, cancelOrderService, reservationService);
            
            long traderId = 2L; // arjun
            long clientId = 7L; // sanjay
            int instrumentId = 1; // AAPL
            String symbol = "AAPL";
            
            // Test 1: Place a BUY LIMIT order
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
            
            System.out.println("Test 1: Placing BUY LIMIT Order");
            OrderPlacementResult res1 = tradingService.placeOrder(buyOrder);
            System.out.println("Result: " + res1.isSuccess() + " - " + res1.getMessage());
            
            // Test 2: Place a SELL LIMIT order that matches the BUY order
            long sellerTraderId = 3L; // priya
            long sellerClientId = 8L; // neha
            
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
            sellOrder.setPrice(new BigDecimal("140.00")); // Crosses the 150.00 bid
            
            System.out.println("Test 2: Placing SELL LIMIT Order (Should match 5 units)");
            OrderPlacementResult res2 = tradingService.placeOrder(sellOrder);
            System.out.println("Result: " + res2.isSuccess() + " - " + res2.getMessage());
            
            // Test 3: Modify the remaining BUY order
            System.out.println("Test 3: Modifying remaining BUY order (Qty reduction in-place)");
            OrderPlacementResult res3 = modifyOrderService.modify(res1.getOrderId(), new BigDecimal("150.00"), 2, traderId);
            System.out.println("Result: " + res3.isSuccess() + " - " + res3.getMessage());
            
            // Test 4: Cancel the remaining BUY order
            System.out.println("Test 4: Cancelling the modified BUY order");
            cancelOrderService.cancel(res1.getOrderId(), traderId);
            System.out.println("Result: Success if no exception");
            
            // Test 5: FOK Order (should fail if insufficient liquidity)
            Order fokOrder = new Order();
            fokOrder.setSymbol(symbol);
            fokOrder.setTraderId(traderId);
            fokOrder.setClientId(clientId);
            fokOrder.setInstrumentId(instrumentId);
            fokOrder.setSide(Side.BUY);
            fokOrder.setOrderType(OrderType.LIMIT);
            fokOrder.setTimeInForce(TimeInForce.FOK);
            fokOrder.setOriginalQty(500); // More than available
            fokOrder.setRemainingQty(500);
            fokOrder.setPrice(new BigDecimal("150.00"));
            System.out.println("Test 5: FOK BUY LIMIT (Insufficient liquidity)");
            OrderPlacementResult res5 = tradingService.placeOrder(fokOrder);
            System.out.println("Result: " + res5.isSuccess() + " - " + res5.getMessage());
            
            // Test 6: MARKET IOC Order
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
            System.out.println("Test 6: MARKET IOC BUY (Should execute if sellers exist)");
            OrderPlacementResult res6 = tradingService.placeOrder(marketIoc);
            System.out.println("Result: " + res6.isSuccess() + " - " + res6.getMessage());
            
            // Print out the order book
            OrderBook book = cache.getOrderBook(symbol);
            System.out.println("Order Book for " + symbol + ":");
            for (Map.Entry<BigDecimal, PriceLevel> entry : book.getBuyBook().entrySet()) {
                System.out.println("  BID: " + entry.getKey() + " Qty: " + entry.getValue().getTotalQuantity());
            }
            for (Map.Entry<BigDecimal, PriceLevel> entry : book.getSellBook().entrySet()) {
                System.out.println("  ASK: " + entry.getKey() + " Qty: " + entry.getValue().getTotalQuantity());
            }
            
            System.out.println("All tests finished.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.close();
        }
    }
}
