package com.bank.trading.dao;

import com.bank.trading.model.ClientNotification;
import java.util.List;

public interface NotificationDao {
    void sendNotification(long clientId, String message);
    List<ClientNotification> findByClientId(long clientId);
}
