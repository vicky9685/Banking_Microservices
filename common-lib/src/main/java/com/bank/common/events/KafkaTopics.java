package com.bank.common.events;

public final class KafkaTopics {
    private KafkaTopics() {}

    public static final String TRANSFER_COMMANDS = "banking.transfer.commands";
    public static final String TRANSFER_EVENTS = "banking.transfer.events";
    public static final String NOTIFICATIONS = "banking.notifications";
    public static final String CUSTOMER_EVENTS = "banking.customer.events";
}
