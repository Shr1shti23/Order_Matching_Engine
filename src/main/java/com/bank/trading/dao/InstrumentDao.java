package com.bank.trading.dao;

import com.bank.trading.model.Instrument;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface InstrumentDao {
    List<Instrument> findAllActive();
    Optional<Instrument> findBySymbol(String symbol);
    void updateLastTradedPrice(int instrumentId, BigDecimal price, Connection conn) throws SQLException;
}
