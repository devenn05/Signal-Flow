package com.sw.signalFlowBackend.repository;

import com.sw.signalFlowBackend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Long,UserEntity> {
}