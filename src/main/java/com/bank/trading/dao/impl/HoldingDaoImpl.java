package com.bank.trading.dao.impl;

import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.HoldingDao;
import com.bank.trading.model.Holding;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HoldingDaoImpl implements HoldingDao {

    @Override
    public List<Holding> findAll() {
        List<Holding> holdings = new ArrayList<>();
        String sql = "SELECT * FROM holdings";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                holdings.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching holdings", e);
        }
        return holdings;
    }

    @Override
    public void upsert(Holding holding, Connection conn) throws SQLException {
        String sql = "INSERT INTO holdings(client_id, instrument_id, quantity, avg_buy_price) " +
                     "VALUES(?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "avg_buy_price = ((avg_buy_price * quantity) + (VALUES(avg_buy_price) * VALUES(quantity))) / (quantity + VALUES(quantity)), " +
                     "quantity = quantity + VALUES(quantity)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, holding.getClientId());
            stmt.setInt(2, holding.getInstrumentId());
            stmt.setLong(3, holding.getQuantity());
            stmt.setBigDecimal(4, holding.getAvgBuyPrice());
            stmt.executeUpdate();
        }
    }

    @Override
    public void updateQuantity(long holdingId, long newQuantity, BigDecimal newAvgBuyPrice, Connection conn) throws SQLException {
        String sql = "UPDATE holdings SET quantity = ?, avg_buy_price = ? WHERE holding_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, newQuantity);
            stmt.setBigDecimal(2, newAvgBuyPrice);
            stmt.setLong(3, holdingId);
            stmt.executeUpdate();
        }
    }

    private Holding mapRow(ResultSet rs) throws SQLException {
        Holding holding = new Holding();
        holding.setHoldingId(rs.getLong("holding_id"));
        holding.setClientId(rs.getLong("client_id"));
        holding.setInstrumentId(rs.getInt("instrument_id"));
        holding.setQuantity(rs.getLong("quantity"));
        holding.setAvgBuyPrice(rs.getBigDecimal("avg_buy_price"));
        return holding;
    }
}
