package com.qros.shared.transaction;

import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Executes short programmatic transactions so slow external I/O (such as image uploads)
 * can run cleanly outside any open database transaction or JDBC connection.
 */
@Service
public class TransactionExecutor {

    private final TransactionTemplate writeTemplate;
    private final TransactionTemplate readOnlyTemplate;

    @Autowired
    public TransactionExecutor(@NonNull PlatformTransactionManager transactionManager) {
        Objects.requireNonNull(transactionManager, "transactionManager must not be null");
        this.writeTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTemplate.setReadOnly(true);
    }

    private TransactionExecutor() {
        this.writeTemplate = null;
        this.readOnlyTemplate = null;
    }

    /**
     * Direct passthrough executor for pure unit tests running without a Spring TransactionManager.
     */
    public static TransactionExecutor direct() {
        return new TransactionExecutor();
    }

    public <T> T readOnly(@NonNull Supplier<T> action) {
        Objects.requireNonNull(action, "action must not be null");
        if (readOnlyTemplate == null) {
            return action.get();
        }
        return readOnlyTemplate.execute(status -> action.get());
    }

    public <T> T write(@NonNull Supplier<T> action) {
        Objects.requireNonNull(action, "action must not be null");
        if (writeTemplate == null) {
            return action.get();
        }
        return writeTemplate.execute(status -> action.get());
    }

    public void writeWithoutResult(@NonNull Runnable action) {
        Objects.requireNonNull(action, "action must not be null");
        if (writeTemplate == null) {
            action.run();
            return;
        }
        writeTemplate.executeWithoutResult(status -> action.run());
    }
}
