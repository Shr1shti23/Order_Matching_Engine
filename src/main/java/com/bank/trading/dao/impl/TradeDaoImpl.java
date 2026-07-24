package com.bank.trading.dao.impl;

import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.TradeDao;
import com.bank.trading.model.Trade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TradeDaoImpl implements TradeDao {

    @Override
    public long insert(Trade trade, Connection conn) throws SQLException {
        String sql = "INSERT INTO trades (instrument_id, buy_order_id, sell_order_id, price, quantity, executed_at) " +
                     "VALUES (?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, trade.getInstrumentId());
            stmt.setLong(2, trade.getBuyOrderId());
            stmt.setLong(3, trade.getSellOrderId());
            stmt.setBigDecimal(4, trade.getPrice());
            stmt.setLong(5, trade.getQuantity());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    trade.setTradeId(id);
                    return id;
                } else {
                    throw new SQLException("Creating trade failed, no ID obtained.");
                }
            }
        }
    }

    @Override
    public List<Trade> findRecentByInstrument(int instrumentId, int limit) {
        List<Trade> trades = new ArrayList<>();
        String sql = "SELECT * FROM trades WHERE instrument_id = ? ORDER BY executed_at DESC LIMIT ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, instrumentId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    trades.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching recent trades for instrument: " + instrumentId, e);
        }
        return trades;
    }

    @Override
    public List<Trade> findAll() {
        List<Trade> trades = new ArrayList<>();
        String sql = "SELECT * FROM trades ORDER BY executed_at DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                trades.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all trades", e);
        }
        return trades;
    }

    @Override
    public List<Trade> findByClientId(long clientId) {
        List<Trade> trades = new ArrayList<>();
        String sql = "SELECT t.*, inst.symbol, u.username AS trader_name " +
                     "FROM trades t " +
                     "JOIN orders o ON (t.buy_order_id = o.order_id OR t.sell_order_id = o.order_id) AND o.client_id = ? " +
                     "LEFT JOIN users u ON o.trader_id = u.user_id " +
                     "LEFT JOIN instruments inst ON t.instrument_id = inst.instrument_id " +
                     "ORDER BY t.executed_at DESC";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, clientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Trade trade = mapRow(rs);
                    try {
                        trade.setSymbol(rs.getString("symbol"));
                    } catch (SQLException ignore) {}
                    try {
                        trade.setTraderName(rs.getString("trader_name"));
                    } catch (SQLException ignore) {}
                    trades.add(trade);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching trades for client " + clientId, e);
        }
        return trades;
    }

    private Trade mapRow(ResultSet rs) throws SQLException {
        Trade trade = new Trade();
        trade.setTradeId(rs.getLong("trade_id"));
        trade.setInstrumentId(rs.getInt("instrument_id"));
        trade.setBuyOrderId(rs.getLong("buy_order_id"));
        trade.setSellOrderId(rs.getLong("sell_order_id"));
        trade.setPrice(rs.getBigDecimal("price"));
        trade.setQuantity(rs.getLong("quantity"));
        trade.setExecutedAt(rs.getTimestamp("executed_at").toLocalDateTime());
        return trade;
    }
}
