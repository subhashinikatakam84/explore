package com.example.demo.Service;

import com.example.demo.Entity.Subscriber;

import java.util.List;

public interface SubscriberService {
    Subscriber createSubscriber(Subscriber subscriber);
    List<Subscriber> fetchAll();
    Subscriber updateSubscriber(Subscriber subscriber,Integer SId);
    void deleteSubscriberById(Integer SId);


}
