package com.vega.protocol.repository;

import com.vega.protocol.entity.GlobalConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalConfigRepository extends JpaRepository<GlobalConfig, Long> {
}