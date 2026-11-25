package com.javaweb.model.event;

/** Sự kiện nghiệp vụ nội bộ (thay cho message Kafka). Bất biến, chỉ mang dữ liệu. */
public class ActivityEvent {
    public static final String BUILDING_SAVED = "BUILDING_SAVED";
    public static final String CUSTOMER_SAVED = "CUSTOMER_SAVED";
    public static final String BUILDING_ASSIGNED = "BUILDING_ASSIGNED";

    private final String type;
    private final String message;

    public ActivityEvent(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
