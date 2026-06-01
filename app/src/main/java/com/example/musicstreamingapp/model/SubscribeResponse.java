package com.example.musicstreamingapp.model;

import java.io.Serializable;

/**
 * Response của POST /api/subscriptions/subscribe.
 * Backend chỉ trả {"subscriptionId": 123} — sub mới tạo, active=false, pending=true.
 * KHÔNG được hiểu là đã premium; phải tiếp tục flow payment/create.
 */
public class SubscribeResponse implements Serializable {
    private Long subscriptionId;

    public Long getSubscriptionId() { return subscriptionId; }
}
