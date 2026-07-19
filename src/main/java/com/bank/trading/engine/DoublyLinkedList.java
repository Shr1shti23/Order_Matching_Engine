package com.bank.trading.engine;

/** Standalone doubly-linked list for OrderNode traversal across price levels. */
public class DoublyLinkedList {
    
    public void addToTail(PriceLevel level, OrderNode node) {
        level.addToTail(node);
    }

    public void remove(PriceLevel level, OrderNode node) {
        level.remove(node);
    }
}
