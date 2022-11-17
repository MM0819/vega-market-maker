package com.vega.protocol.repository;

import com.vega.protocol.entity.MarketConfig;
import com.vega.protocol.entity.TradingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TradingConfigRepository extends JpaRepository<TradingConfig, UUID> {
    Optional<TradingConfig> findByMarketConfig(MarketConfig marketConfig);
}