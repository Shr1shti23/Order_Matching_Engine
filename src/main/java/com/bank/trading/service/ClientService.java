package com.bank.trading.service;

import com.bank.trading.cache.CacheManager;
import com.bank.trading.dao.OrderDao;
import com.bank.trading.dao.TradeDao;
import com.bank.trading.dao.UserDao;
import com.bank.trading.dao.WalletTransactionDao;
import com.bank.trading.dao.impl.OrderDaoImpl;
import com.bank.trading.dao.impl.TradeDaoImpl;
import com.bank.trading.dao.impl.UserDaoImpl;
import com.bank.trading.dao.impl.WalletTransactionDaoImpl;
import com.bank.trading.model.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides read-only self-service views for clients.
 */
public final class ClientService {

    private final CacheManager            cache;
    private final OrderDao                orderDao;
    private final TradeDao                tradeDao;
    private final WalletTransactionDao    walletTxDao;
    private final UserDao                 userDao;

    public ClientService(CacheManager cache) {
        this.cache       = cache;
        this.orderDao    = new OrderDaoImpl();
        this.tradeDao    = new TradeDaoImpl();
        this.walletTxDao = new WalletTransactionDaoImpl();
        this.userDao     = new UserDaoImpl();
    }

    // ================================================================== //
    //  Portfolio                                                          //
    // ================================================================== //

    public Map<Integer, Holding> getHoldings(long clientId) {
        return cache.getHoldingsForClient(clientId);
    }

    // ================================================================== //
    //  Wallet                                                             //
    // ================================================================== //

    public Wallet getWallet(long clientId) {
        return cache.getWallet(clientId);
    }

    public List<WalletTransaction> getWalletTransactions(long clientId) {
        Wallet wallet = cache.getWallet(clientId);
        if (wallet == null) return List.of();
        return walletTxDao.findByWalletId(wallet.getWalletId());
    }

    // ================================================================== //
    //  Orders                                                             //
    // ================================================================== //

    public List<Order> getOrders(long clientId) {
        return orderDao.findByClientId(clientId);
    }

    // ================================================================== //
    //  Trades                                                             //
    // ================================================================== //

    public List<Trade> getTrades(long clientId) {
        return tradeDao.findByClientId(clientId);
    }

    // ================================================================== //
    //  Profile                                                            //
    // ================================================================== //

    public Optional<User> getProfile(long clientId) {
        return userDao.findById(clientId);
    }

    // ================================================================== //
    //  Instruments                                                        //
    // ================================================================== //

    public Collection<Instrument> getInstruments() {
        return cache.getAllInstruments();
    }
}
