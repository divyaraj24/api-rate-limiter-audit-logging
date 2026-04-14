package com.example.apitool.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditProperties properties;
    private final ObjectMapper objectMapper;
    private final BlockingQueue<AuditEvent> queue = new LinkedBlockingQueue<>(20_000);
    private final AtomicBoolean running = new AtomicBoolean(true);

    private Thread writerThread;

    public AuditLogService(AuditProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unused")
    @PostConstruct
    void startWriter() {
        writerThread = new Thread(this::writeLoop, "audit-log-writer");
        writerThread.setDaemon(true);
        writerThread.start();
    }

    public void append(AuditEvent event) {
        if (!queue.offer(event)) {
            log.warn("Audit queue full; dropping event for user={} endpoint={}", event.userId(), event.endpoint());
        }
    }

    private void writeLoop() {
        Path path = Path.of(properties.getFilePath());
        try {
            Files.createDirectories(path.getParent() == null ? Path.of(".") : path.getParent());
        } catch (IOException e) {
            log.error("Failed creating audit log directory", e);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND)) {
            while (running.get() || !queue.isEmpty()) {
                AuditEvent event = queue.poll(1, TimeUnit.SECONDS);
                if (event == null) {
                    continue;
                }
                writer.write(objectMapper.writeValueAsString(event));
                writer.newLine();
                writer.flush();
            }
        } catch (Exception e) {
            log.error("Audit writer stopped unexpectedly", e);
        }
    }

    @SuppressWarnings("unused")
    @PreDestroy
    void stopWriter() {
        running.set(false);
        if (writerThread != null) {
            writerThread.interrupt();
            try {
                writerThread.join(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
