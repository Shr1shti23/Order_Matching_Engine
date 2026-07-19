package com.bank.trading.dao;

import com.bank.trading.model.TraderClientAssignment;
import java.util.List;

public interface TraderAssignmentDao {
    List<TraderClientAssignment> findAllActive();
}
