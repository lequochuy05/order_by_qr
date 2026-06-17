package com.qros.modules.table.repository;

import com.qros.modules.table.model.TableSessionToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TableSessionTokenRepository extends JpaRepository<TableSessionToken, Long> {

    @EntityGraph(attributePaths = { "session", "session.table" })
    Optional<TableSessionToken> findFirstByTokenHashAndRevokedAtIsNull(String tokenHash);
}
