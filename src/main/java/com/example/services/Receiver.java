package com.example.services;

/**
 * The receiver receives, processes an event and tells the FileManger what to do
 */
public interface Receiver {

    /**
     * Receive an event
     * @param event The event
     */
    public void receive(Event event);
}
