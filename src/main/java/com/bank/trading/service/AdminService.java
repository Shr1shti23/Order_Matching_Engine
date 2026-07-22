package com.bank.trading.service;

import com.bank.trading.cache.CacheManager;
import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.*;
import com.bank.trading.dao.impl.*;
import com.bank.trading.model.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates all administrative operations for the Bank Trading Platform.
 *
 * All database mutations run within explicit JDBC transactions.
 * The matching-engine caches (order books, resting orders) are never touched here.
 */
public final class AdminService {

    private final CacheManager            cache;
    private final UserDao                 userDao;
    private final TraderProfileDao        traderProfileDao;
    private final ClientProfileDao        clientProfileDao;
    private final TraderAssignmentDao     assignmentDao;
    private final InstrumentDao           instrumentDao;
    private final WalletDao               walletDao;
    private final HoldingDao              holdingDao;
    private final HoldingTransactionDao   holdingTxDao;
    private final OrderDao                orderDao;
    private final TradeDao                tradeDao;
    private final AuditLogDao             auditLogDao;

    public AdminService(CacheManager cache) {
        this.cache            = cache;
        this.userDao          = new UserDaoImpl();
        this.traderProfileDao = new TraderProfileDaoImpl();
        this.clientProfileDao = new ClientProfileDaoImpl();
        this.assignmentDao    = new TraderAssignmentDaoImpl();
        this.instrumentDao    = new InstrumentDaoImpl();
        this.walletDao        = new WalletDaoImpl();
        this.holdingDao       = new HoldingDaoImpl();
        this.holdingTxDao     = new HoldingTransactionDaoImpl();
        this.orderDao         = new OrderDaoImpl();
        this.tradeDao         = new TradeDaoImpl();
        this.auditLogDao      = new AuditLogDaoImpl();
    }

    // ================================================================== //
    //  TRADER MANAGEMENT                                                  //
    // ================================================================== //

    public User createTrader(String username, String email, String password,
                             String employeeCode, String department, long adminId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                User user = new User();
                user.setUsername(username);
                user.setEmail(email);
                user.setPasswordHash(password);
                user.setPasswordAlgo("plain");
                user.setRoleId(2);
                user.setStatus("ACTIVE");
                user.setCreatedBy(adminId);
                userDao.save(user, conn);

                Trader trader = new Trader();
                trader.setUserId(user.getUserId());
                trader.setEmployeeCode(employeeCode);
                trader.setDepartment(department);
                traderProfileDao.save(trader, conn);

                conn.commit();
                return user;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to create trader: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error creating trader", e);
        }
    }

    public void suspendTrader(long traderId, long adminId) {
        setUserStatus(traderId, "SUSPENDED");
    }

    public void activateTrader(long traderId, long adminId) {
        setUserStatus(traderId, "ACTIVE");
    }

    public void deleteTrader(long traderId, long adminId) {
        setUserStatus(traderId, "DELETED");
    }

    public List<User> viewAllTraders() {
        return userDao.findByRoleId(2);
    }

    // ================================================================== //
    //  CLIENT MANAGEMENT                                                  //
    // ================================================================== //

    public User createClient(String username, String email, String password,
                             String kycStatus, String riskProfile,
                             BigDecimal initialBalance, String currency,
                             long adminId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                User user = new User();
                user.setUsername(username);
                user.setEmail(email);
                user.setPasswordHash(password);
                user.setPasswordAlgo("plain");
                user.setRoleId(3);
                user.setStatus("ACTIVE");
                user.setCreatedBy(adminId);
                userDao.save(user, conn);

                Client client = new Client();
                client.setUserId(user.getUserId());
                client.setKycStatus(kycStatus);
                client.setRiskProfile(riskProfile);
                clientProfileDao.save(client, conn);

                Optional<Wallet> existing = walletDao.findByClientId(user.getUserId());
                if (existing.isEmpty()) {
                    Wallet wallet = new Wallet();
                    wallet.setClientId(user.getUserId());
                    wallet.setCashBalance(initialBalance);
                    wallet.setReservedBalance(BigDecimal.ZERO);
                    wallet.setCurrency(currency);
                    walletDao.save(wallet, conn);
                }

                conn.commit();
                cache.refreshWallets();
                return user;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to create client: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error creating client", e);
        }
    }

    public void updateClient(long clientId, String kycStatus, String riskProfile) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Client client = new Client();
                client.setUserId(clientId);
                client.setKycStatus(kycStatus);
                client.setRiskProfile(riskProfile);
                clientProfileDao.update(client, conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to update client: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating client", e);
        }
    }

    public void deleteClient(long clientId, long adminId) {
        setUserStatus(clientId, "DELETED");
    }

    public List<User> viewAllClients() {
        return userDao.findByRoleId(3);
    }

    // ================================================================== //
    //  CLIENT PORTFOLIO INITIALIZATION                                    //
    // ================================================================== //

    public void initializeHolding(long clientId, String symbol,
                                  String instrumentName, InstrumentType instrumentType,
                                  BigDecimal tickSize, int lotSize,
                                  long quantity, BigDecimal avgBuyPrice, long adminId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Optional<Instrument> opt = instrumentDao.findBySymbol(symbol);
                Instrument instrument;
                if (opt.isPresent()) {
                    instrument = opt.get();
                } else {
                    instrument = new Instrument();
                    instrument.setSymbol(symbol.toUpperCase());
                    instrument.setName(instrumentName);
                    instrument.setInstrumentType(instrumentType);
                    instrument.setTickSize(tickSize);
                    instrument.setLotSize(lotSize);
                    instrument.setStatus(InstrumentStatus.ACTIVE);
                    instrument.setLastTradedPrice(avgBuyPrice);
                    instrument.setCreatedBy(adminId);
                    instrumentDao.save(instrument, conn);
                }

                Holding holding = new Holding();
                holding.setClientId(clientId);
                holding.setInstrumentId(instrument.getInstrumentId());
                holding.setQuantity(quantity);
                holding.setAvgBuyPrice(avgBuyPrice);
                holdingDao.upsert(holding, conn);

                HoldingTransaction txn = new HoldingTransaction();
                txn.setClientId(clientId);
                txn.setInstrumentId(instrument.getInstrumentId());
                txn.setTradeId(null);
                txn.setTransactionType(HoldingTxType.ADJUSTMENT);
                txn.setQuantity(quantity);
                txn.setPrice(avgBuyPrice);
                holdingTxDao.insert(txn, conn);

                conn.commit();
                cache.refreshInstruments();
                cache.refreshHoldings();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to initialize holding: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error initializing holding", e);
        }
    }

    // ================================================================== //
    //  TRADER-CLIENT ASSIGNMENTS                                          //
    // ================================================================== //

    public void assignClientToTrader(long traderId, long clientId, long adminId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                assignmentDao.assign(traderId, clientId, adminId, conn);
                conn.commit();
                cache.refreshTraderAssignments();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to assign client: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error assigning client", e);
        }
    }

    public void reassignClient(long oldTraderId, long newTraderId, long clientId, long adminId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                assignmentDao.reassign(oldTraderId, newTraderId, clientId, adminId, conn);
                conn.commit();
                cache.refreshTraderAssignments();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to reassign client: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error reassigning client", e);
        }
    }

    public void removeAssignment(long traderId, long clientId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                assignmentDao.deassign(traderId, clientId, conn);
                conn.commit();
                cache.refreshTraderAssignments();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to remove assignment: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error removing assignment", e);
        }
    }

    public List<TraderClientAssignment> viewAllAssignments() {
        return assignmentDao.findAll();
    }

    // ================================================================== //
    //  INSTRUMENT MANAGEMENT                                              //
    // ================================================================== //

    public Instrument registerInstrument(String symbol, String name,
                                          InstrumentType instrumentType,
                                          BigDecimal tickSize, int lotSize, long adminId) {
        Optional<Instrument> opt = instrumentDao.findBySymbol(symbol);
        if (opt.isPresent()) return opt.get();

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Instrument instrument = new Instrument();
                instrument.setSymbol(symbol.toUpperCase());
                instrument.setName(name);
                instrument.setInstrumentType(instrumentType);
                instrument.setTickSize(tickSize);
                instrument.setLotSize(lotSize);
                instrument.setStatus(InstrumentStatus.ACTIVE);
                instrument.setCreatedBy(adminId);
                instrumentDao.save(instrument, conn);
                conn.commit();
                cache.refreshInstruments();
                return instrument;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to register instrument: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error registering instrument", e);
        }
    }

    public void updateInstrument(int instrumentId, String name,
                                  InstrumentType type, BigDecimal tickSize, int lotSize) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Instrument instrument = new Instrument();
                instrument.setInstrumentId(instrumentId);
                instrument.setName(name);
                instrument.setInstrumentType(type);
                instrument.setTickSize(tickSize);
                instrument.setLotSize(lotSize);
                instrumentDao.update(instrument, conn);
                conn.commit();
                cache.refreshInstruments();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to update instrument: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error updating instrument", e);
        }
    }

    public void activateInstrument(int instrumentId) {
        setInstrumentStatus(instrumentId, InstrumentStatus.ACTIVE);
    }

    public void deactivateInstrument(int instrumentId) {
        setInstrumentStatus(instrumentId, InstrumentStatus.SUSPENDED);
    }

    public List<Instrument> viewAllInstruments() {
        return instrumentDao.findAll();
    }

    // ================================================================== //
    //  SYSTEM MONITORING                                                  //
    // ================================================================== //

    public List<Order> viewAllOrders() {
        return orderDao.findAll();
    }

    public List<Order> viewActiveOrders() {
        return orderDao.findOpenOrders();
    }

    public List<Trade> viewAllTrades() {
        return tradeDao.findAll();
    }

    public List<AuditLog> viewAuditLogs(int limit) {
        return auditLogDao.findRecent(limit);
    }

    public Map<String, Object> viewDashboard() {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT * FROM v_admin_dashboard";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                stats.put("total_users",    rs.getLong("total_users"));
                stats.put("total_traders",  rs.getLong("total_traders"));
                stats.put("total_clients",  rs.getLong("total_clients"));
                stats.put("active_traders", rs.getLong("active_traders"));
                stats.put("trades_today",   rs.getLong("trades_today"));
                stats.put("turnover_today", rs.getBigDecimal("turnover_today"));
            }
        } catch (SQLException e) {
            // Dashboard view may not exist — return partial stats
            stats.put("error", e.getMessage());
        }
        return stats;
    }

    // ================================================================== //
    //  Private helpers                                                    //
    // ================================================================== //

    private void setUserStatus(long userId, String status) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                userDao.updateStatus(userId, status, conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to set user status: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error setting user status", e);
        }
    }

    private void setInstrumentStatus(int instrumentId, InstrumentStatus status) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                instrumentDao.updateStatus(instrumentId, status, conn);
                conn.commit();
                cache.refreshInstruments();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to set instrument status: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error setting instrument status", e);
        }
    }
}
