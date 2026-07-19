package com.bank.trading.dao;

import com.bank.trading.model.Trade;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface TradeDao {
    long insert(Trade trade, Connection conn) throws SQLException;
    List<Trade> findRecentByInstrument(int instrumentId, int limit);
}
