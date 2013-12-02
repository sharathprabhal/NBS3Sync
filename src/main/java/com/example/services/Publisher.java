package com.example.services;

/**
 * The Publisher is responsible for publishing events
 */
public interface Publisher {

    /**
     * Publish the event
     * @param event The event
     */
    public void publish(Event event);

}
