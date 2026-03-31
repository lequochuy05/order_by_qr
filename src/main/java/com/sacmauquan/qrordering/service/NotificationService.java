package com.sacmauquan.qrordering.service;

public interface NotificationService {
    void notifyOrderChange();
    void notifyMenuChange(String type, Object id);
    void notifyTableChange();
    void notifyCategoryChange(String event, Object id);
    void notifyComboChange(String event, Object id);
}
