package com.example.mocks;

import com.example.services.Event;
import com.example.services.Publisher;

import java.util.ArrayList;
import java.util.List;

public class MockPublisher implements Publisher {

    public List<Event> events = new ArrayList<Event>();

    @Override
    public void publish(Event event) {
        events.add(event);
    }
}
