package com.bank.trading.dao;

import com.bank.trading.model.Holding;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface HoldingDao {
    List<Holding> findAll();
    void upsert(Holding holding, Connection conn) throws SQLException;
    void updateQuantity(long holdingId, long newQuantity, BigDecimal newAvgBuyPrice, Connection conn) throws SQLException;
}
