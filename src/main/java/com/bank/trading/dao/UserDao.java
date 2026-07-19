package com.bank.trading.dao;

import com.bank.trading.model.User;
import java.util.Optional;

public interface UserDao {
    Optional<User> findById(long userId);
    Optional<User> findByUsername(String username);
}
