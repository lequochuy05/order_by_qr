package com.qros.shared.transaction;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
public class TransactionSideEffectService {

    public void afterCommit(Runnable action, String description) {
        register(action, description, true);
    }

    public void afterRollback(Runnable action, String description) {
        register(action, description, false);
    }

    private void register(Runnable action, String description, boolean commitPhase) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runSafely(action, description);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (commitPhase) {
                    runSafely(action, description);
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (!commitPhase && status == STATUS_ROLLED_BACK) {
                    runSafely(action, description);
                }
            }
        });
    }

    private void runSafely(Runnable action, String description) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("Side effect failed: {}", description, e);
        }
    }
}
