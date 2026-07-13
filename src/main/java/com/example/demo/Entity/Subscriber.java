package com.example.demo.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.PostPersist;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
//@Slf4j
public class Subscriber {
    private static final Logger log = LoggerFactory.getLogger(Subscriber.class);

    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer SId;
    private String Sname;
    private String Saddr;

//    @PostPersist
//    public void logNewSubscriber() {
//        log.info("[AUDIT] New subscriber created in database with generated ID: {}", this.SId);
//    }

    public Integer getSId() {
        return SId;
    }

    public void setSId(Integer SId) {
        this.SId = SId;
    }

    public String getSname() {
        return Sname;
    }

    public void setSname(String sname) {
        Sname = sname;
    }

    public String getSaddr() {
        return Saddr;
    }

    public void setSaddr(String saddr) {
        Saddr = saddr;
    }
}
