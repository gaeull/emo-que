package com.emoque.repository;

import com.emoque.model.GenerationTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationTaskRepository extends JpaRepository<GenerationTask, String> {
}
