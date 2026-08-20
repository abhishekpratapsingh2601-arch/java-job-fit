package com.javajobfit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JavaJobFitApplication {
    private static final Logger log = LoggerFactory.getLogger(JavaJobFitApplication.class);

    public static void main(String[] args) {
        // First line of every boot in the Render logs. maxMemory must read ~192MB (the -Xmx);
        // a multi-GB value here means the runtime hid the cgroup limit from the JVM — the
        // failure mode behind the exit-137 kills (see Dockerfile).
        log.info("JVM sees: maxHeap={}MB, processors={}",
                Runtime.getRuntime().maxMemory() / (1024 * 1024),
                Runtime.getRuntime().availableProcessors());
        SpringApplication.run(JavaJobFitApplication.class, args);
    }
}
