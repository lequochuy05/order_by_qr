package com.qros.modules.table.repository;

import com.qros.modules.table.model.TableSessionToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableSessionTokenRepository extends JpaRepository<TableSessionToken, Long> {

    @EntityGraph(attributePaths = {"session", "session.table"})
    Optional<TableSessionToken> findFirstByTokenHashAndRevokedAtIsNull(String tokenHash);
}
