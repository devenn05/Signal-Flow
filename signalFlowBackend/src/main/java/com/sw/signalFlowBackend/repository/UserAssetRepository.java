package com.sw.signalFlowBackend.repository;

import com.sw.signalFlowBackend.entity.UserAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAssetRepository extends JpaRepository<UserAsset, Long> {
    List<UserAsset> findByUserId(Long userId);
    Optional<UserAsset> findByUserIdAndSymbol(Long userId, String symbol);
}
