package com.bank.trading.dao.impl;

import com.bank.trading.config.DatabaseConfig;
import com.bank.trading.dao.InstrumentDao;
import com.bank.trading.model.Instrument;
import com.bank.trading.model.InstrumentStatus;
import com.bank.trading.model.InstrumentType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InstrumentDaoImpl implements InstrumentDao {

    @Override
    public List<Instrument> findAllActive() {
        List<Instrument> instruments = new ArrayList<>();
        String sql = "SELECT * FROM instruments WHERE status = 'ACTIVE'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                instruments.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching active instruments", e);
        }
        return instruments;
    }

    @Override
    public Optional<Instrument> findBySymbol(String symbol) {
        String sql = "SELECT * FROM instruments WHERE symbol = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, symbol);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding instrument by symbol: " + symbol, e);
        }
        return Optional.empty();
    }

    @Override
    public void updateLastTradedPrice(int instrumentId, BigDecimal price, Connection conn) throws SQLException {
        String sql = "UPDATE instruments SET last_traded_price = ? WHERE instrument_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, price);
            stmt.setInt(2, instrumentId);
            stmt.executeUpdate();
        }
    }

    private Instrument mapRow(ResultSet rs) throws SQLException {
        Instrument instrument = new Instrument();
        instrument.setInstrumentId(rs.getInt("instrument_id"));
        instrument.setSymbol(rs.getString("symbol"));
        instrument.setName(rs.getString("name"));
        instrument.setInstrumentType(InstrumentType.valueOf(rs.getString("instrument_type")));
        instrument.setTickSize(rs.getBigDecimal("tick_size"));
        instrument.setLotSize(rs.getInt("lot_size"));
        instrument.setStatus(InstrumentStatus.valueOf(rs.getString("status")));
        instrument.setLastTradedPrice(rs.getBigDecimal("last_traded_price"));
        
        long createdBy = rs.getLong("created_by");
        if (!rs.wasNull()) {
            instrument.setCreatedBy(createdBy);
        }
        return instrument;
    }
}
