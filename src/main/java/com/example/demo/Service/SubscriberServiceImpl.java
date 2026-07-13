package com.example.demo.Service;

import com.example.demo.Entity.Subscriber;
import com.example.demo.Repository.SubscriberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
// @Slf4j
public class SubscriberServiceImpl implements SubscriberService{
    private static final Logger log = LoggerFactory.getLogger(SubscriberServiceImpl.class);

    @Autowired
    SubscriberRepository repository;
    @Override
    @Transactional
    public Subscriber createSubscriber(Subscriber subscriber) {
        log.info("Persisting new subscriber to the database");
        return repository.save(subscriber);
    }

    @Override
    public List<Subscriber> fetchAll() {

        log.info("Fetching all subscriber profiles from repository");
        return (List<Subscriber>) repository.findAll();
    }

    @Override
    public Subscriber updateSubscriber(Subscriber subscriber, Integer SId) {

        Subscriber subDB
                = repository.findById(SId)
                .get();
        if (subscriber.getSId().equals(subDB.getSId())) {
            return repository.save(subscriber);
        }
        System.out.println("No record found in db to update");
        log.warn("Update failed: No record found in db for Subscriber ID: {}", SId);
        return null;
        //return null;
    }

    @Override
    public void deleteSubscriberById(Integer SId) {
        log.warn("Executing database delete transaction for Subscriber ID: {}", SId);

        try {
            repository.deleteById(SId);
            log.info("Successfully dropped database record for Subscriber ID: {}", SId);
        } catch (Exception e) {
            // 7. Track stack traces correctly by putting 'e' as the last parameter
            log.error("Failed to delete subscriber with ID: {} due to system error", SId, e);
            throw e;
        }

        //repository.deleteById(SId);
    }
}
