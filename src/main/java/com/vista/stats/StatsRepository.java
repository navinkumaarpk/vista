package com.vista.stats;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface StatsRepository extends MongoRepository<StatsDocument, String> {
    List<StatsDocument> findByServiceGroupOrderByTimestampDesc(String serviceGroup);
    Optional<StatsDocument> findFirstByServiceGroupOrderByTimestampDesc(String serviceGroup);
}