package com.example.quanlydetaisinhvien.service.notification;

import com.example.quanlydetaisinhvien.config.NotificationEmailSettings;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import java.util.concurrent.*;

/** Dedicated scheduler; deliberately does not enable Spring's global scheduling/Module 16 tasks. */
@Component
public class NotificationEmailScheduler implements SmartLifecycle {
    private final NotificationEmailWorker worker;
    private final NotificationEmailSafety safety;
    private final NotificationEmailSettings settings;
    private ScheduledExecutorService executor;
    public NotificationEmailScheduler(NotificationEmailWorker worker, NotificationEmailSafety safety, NotificationEmailSettings settings) {
        this.worker=worker; this.safety=safety; this.settings=settings;
    }
    @Override public synchronized void start() {
        if (executor != null || !settings.scheduleEnabled() || !safety.deliveryAllowed()) return;
        executor=Executors.newSingleThreadScheduledExecutor(r -> { var t=new Thread(r,"notification-email"); t.setDaemon(true); return t; });
        executor.scheduleWithFixedDelay(worker::drain, settings.pollMs(), settings.pollMs(), TimeUnit.MILLISECONDS);
    }
    @Override public synchronized void stop() { if (executor != null) { executor.shutdownNow(); executor=null; } }
    @Override public synchronized boolean isRunning() { return executor != null && !executor.isShutdown(); }
}
