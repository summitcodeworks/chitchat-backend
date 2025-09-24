package com.chitchat.shared.repository;

import com.chitchat.shared.entity.ApplicationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ApplicationConfig entity
 */
@Repository
public interface ApplicationConfigRepository extends JpaRepository<ApplicationConfig, Long> {

    /**
     * Find configuration by key
     */
    Optional<ApplicationConfig> findByConfigKey(String configKey);

    /**
     * Find all configurations for a specific service
     */
    List<ApplicationConfig> findByService(String service);

    /**
     * Find all configurations for multiple services
     */
    @Query("SELECT ac FROM ApplicationConfig ac WHERE ac.service IN :services")
    List<ApplicationConfig> findByServiceIn(@Param("services") List<String> services);

    /**
     * Check if configuration exists by key
     */
    boolean existsByConfigKey(String configKey);

    /**
     * Find configurations by key pattern
     */
    @Query("SELECT ac FROM ApplicationConfig ac WHERE ac.configKey LIKE %:pattern%")
    List<ApplicationConfig> findByConfigKeyContaining(@Param("pattern") String pattern);

    /**
     * Find configurations by service and key pattern
     */
    @Query("SELECT ac FROM ApplicationConfig ac WHERE ac.service = :service AND ac.configKey LIKE %:pattern%")
    List<ApplicationConfig> findByServiceAndConfigKeyContaining(@Param("service") String service, @Param("pattern") String pattern);

    /**
     * Delete configuration by key
     */
    void deleteByConfigKey(String configKey);
}
