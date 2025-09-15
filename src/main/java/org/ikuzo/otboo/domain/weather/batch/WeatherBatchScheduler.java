package org.ikuzo.otboo.domain.weather.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class WeatherBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job weatherCollectJob;

    @Value("${weather.batch.enabled:true}")
    private boolean enabled;

    @Value("${weather.batch.cron:}")
    private String cron;
    
    @Scheduled(cron = "${weather.batch.cron:0 0/30 * * * *}", zone = "Asia/Seoul")
    public void run() {
        if (!enabled) {
            return;
        }
        try {
            JobParameters params = new JobParametersBuilder()
                .addLong("ts", System.currentTimeMillis())
                .toJobParameters();
            jobLauncher.run(weatherCollectJob, params);
        } catch (Exception e) {
            log.error("Weather batch run error", e);
        }
    }
}