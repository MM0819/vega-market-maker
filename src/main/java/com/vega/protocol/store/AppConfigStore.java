package com.vega.protocol.store;

import com.vega.protocol.model.AppConfig;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;

@Repository
public class AppConfigStore extends SingleItemStore<AppConfig> {
    @PostConstruct
    public void initialize() {
        update(new AppConfig());
    }
}