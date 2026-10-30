package com.rasitbank.retrieval.workspace.repository;

import com.rasitbank.retrieval.workspace.domain.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Çalışma alanı kayıtlarını okur.
 */
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
}
