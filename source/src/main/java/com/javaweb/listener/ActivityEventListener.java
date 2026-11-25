package com.javaweb.listener;

import com.javaweb.model.event.ActivityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ActivityEventListener {
    private static final Logger log = LoggerFactory.getLogger(ActivityEventListener.class);

    /**
     * AFTER_COMMIT: chỉ chạy khi transaction MySQL đã commit thành công
     *   -> rollback thì KHÔNG có log "ma" (tránh dual-write).
     * @Async: không kéo dài thời gian phản hồi của request.
     * Lưu ý: ở pha AFTER_COMMIT transaction đã đóng; nếu muốn ghi bảng audit_log
     * phải mở transaction mới (REQUIRES_NEW).
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onActivity(ActivityEvent event) {
        log.info("[ACTIVITY] {} - {}", event.getType(), event.getMessage());
    }
}
