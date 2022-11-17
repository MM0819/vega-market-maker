package com.vega.protocol.repository;

import com.vega.protocol.entity.MarketConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MarketConfigRepository extends JpaRepository<MarketConfig, UUID> {
}