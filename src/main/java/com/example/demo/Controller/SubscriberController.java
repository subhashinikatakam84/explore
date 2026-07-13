package com.example.demo.Controller;

import com.example.demo.Entity.Subscriber;
import com.example.demo.Service.SubscriberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

@RestController
@RequestMapping("/api/subscriber")
//@Slf4j
public class SubscriberController {
    private static final Logger log = LoggerFactory.getLogger(SubscriberController.class);

    @Autowired
    SubscriberService service;
    @PostMapping("/create")
    public Subscriber createSubscriber(@RequestBody Subscriber subscriber){
        log.info("REST request to create subscriber with name: {}", subscriber.getSname());
        log.info("subscriber created with name: {}");


        return service.createSubscriber(subscriber);
    }

@GetMapping("/getAll")

    public List<Subscriber> fetchAll(){

    long startTime = System.currentTimeMillis();
    log.info("REST request to fetch all subscribers");

    List<Subscriber> subscribers = service.fetchAll();

    long duration = System.currentTimeMillis() - startTime;
    log.info("Successfully fetched {} subscribers in {} ms", subscribers.size(), duration);

    return subscribers;

     //return service.fetchAll();
}
@PutMapping("/update/{id}")

    public Subscriber updateSubscriber(@RequestBody Subscriber subscriber,
                                       @PathVariable("id") Integer SId){
    log.info("REST request to update subscriber ID: {} with new values", SId);
    return service.updateSubscriber(subscriber, SId);

   //return service.updateSubscriber(subscriber,SId);

}
@DeleteMapping("/delete/{id}")
    public String deleteSubscriberById(@PathVariable("id") Integer SId){
    log.warn("REST request to delete subscriber ID: {}", SId);

    service.deleteSubscriberById(SId);

    log.info("Successfully deleted subscriber ID: {}", SId);
    return "deleted Successfully";


    }


}
