package com.bank.trading.dao.impl;

import com.bank.trading.dao.TraderProfileDao;
import com.bank.trading.model.Trader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TraderProfileDaoImpl implements TraderProfileDao {

    @Override
    public void save(Trader trader, Connection conn) throws SQLException {
        String sql = "INSERT INTO traders (user_id, employee_code, department) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, trader.getUserId());
            stmt.setString(2, trader.getEmployeeCode());
            stmt.setString(3, trader.getDepartment());
            stmt.executeUpdate();
        }
    }
}
