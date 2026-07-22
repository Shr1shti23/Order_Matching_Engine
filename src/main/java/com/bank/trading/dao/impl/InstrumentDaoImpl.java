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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InstrumentDaoImpl implements InstrumentDao {

    @Override
    public List<Instrument> findAllActive() {
        List<Instrument> instruments = new ArrayList<>();
        String sql = "SELECT * FROM instruments WHERE status = 'ACTIVE' ORDER BY symbol";
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
    public List<Instrument> findAll() {
        List<Instrument> instruments = new ArrayList<>();
        String sql = "SELECT * FROM instruments ORDER BY symbol";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                instruments.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching instruments", e);
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
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding instrument by symbol: " + symbol, e);
        }
        return Optional.empty();
    }

    @Override
    public void save(Instrument instrument, Connection conn) throws SQLException {
        String sql = "INSERT INTO instruments (symbol, name, instrument_type, tick_size, lot_size, status, last_traded_price, created_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, instrument.getSymbol());
            stmt.setString(2, instrument.getName());
            stmt.setString(3, instrument.getInstrumentType().name());
            stmt.setBigDecimal(4, instrument.getTickSize());
            stmt.setInt(5, instrument.getLotSize());
            stmt.setString(6, instrument.getStatus().name());
            stmt.setBigDecimal(7, instrument.getLastTradedPrice());
            if (instrument.getCreatedBy() != 0) {
                stmt.setLong(8, instrument.getCreatedBy());
            } else {
                stmt.setNull(8, java.sql.Types.BIGINT);
            }
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    instrument.setInstrumentId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void update(Instrument instrument, Connection conn) throws SQLException {
        String sql = "UPDATE instruments SET name = ?, instrument_type = ?, tick_size = ?, lot_size = ? " +
                     "WHERE instrument_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, instrument.getName());
            stmt.setString(2, instrument.getInstrumentType().name());
            stmt.setBigDecimal(3, instrument.getTickSize());
            stmt.setInt(4, instrument.getLotSize());
            stmt.setInt(5, instrument.getInstrumentId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void updateStatus(int instrumentId, InstrumentStatus status, Connection conn) throws SQLException {
        String sql = "UPDATE instruments SET status = ? WHERE instrument_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, instrumentId);
            stmt.executeUpdate();
        }
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
        BigDecimal lastPrice = rs.getBigDecimal("last_traded_price");
        if (!rs.wasNull()) instrument.setLastTradedPrice(lastPrice);
        instrument.setCreatedBy(rs.getLong("created_by"));
        return instrument;
    }
}
