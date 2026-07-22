package com.bank.trading.dao;

import com.bank.trading.model.Instrument;
import com.bank.trading.model.InstrumentStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface InstrumentDao {
    /** Returns all ACTIVE instruments (used by CacheManager startup). */
    List<Instrument> findAllActive();
    /** Returns all instruments regardless of status (used by AdminService). */
    List<Instrument> findAll();
    Optional<Instrument> findBySymbol(String symbol);
    void save(Instrument instrument, Connection conn) throws SQLException;
    void update(Instrument instrument, Connection conn) throws SQLException;
    void updateStatus(int instrumentId, InstrumentStatus status, Connection conn) throws SQLException;
    /** Updates the last_traded_price column (used by SettlementService). */
    void updateLastTradedPrice(int instrumentId, BigDecimal price, Connection conn) throws SQLException;
}
