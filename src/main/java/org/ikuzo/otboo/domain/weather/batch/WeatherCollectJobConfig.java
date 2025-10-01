package org.ikuzo.otboo.domain.weather.batch;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ikuzo.otboo.domain.user.entity.User;
import org.ikuzo.otboo.domain.user.repository.UserRepository;
import org.ikuzo.otboo.domain.weather.service.WeatherServiceImpl;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class WeatherCollectJobConfig {

    private final UserRepository userRepository;
    private final WeatherServiceImpl weatherServiceImpl;

    // Job
    @Bean
    public Job weatherCollectJob(JobRepository jobRepository, Step collectWeathersStep) {
        return new JobBuilder("weatherCollectJob", jobRepository)
            .start(collectWeathersStep)
            .build();
    }

    // Step
    @Bean
    public Step collectWeathersStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    Tasklet collectTasklet) {
        return new StepBuilder("collectWeathersStep", jobRepository)
            .tasklet(collectTasklet, transactionManager)
            .build();
    }

    // Tasklet
    @Bean
    public Tasklet collectTasklet() {
        return (contribution, chunkContext) -> {
            List<User> targets =
                userRepository.findByLockedFalseAndLatitudeIsNotNullAndLongitudeIsNotNull();

            if (targets.isEmpty()) {
                log.warn("[WeatherCollectJobConfig] 조건을 만족하는 사용자가 없어 배치를 종료합니다.");
                return RepeatStatus.FINISHED;
            }

            int success = 0, fail = 0;
            for (User u : targets) {
                try {
                    weatherServiceImpl.collectAndSaveForUser(u.getId());
                    success++;
                } catch (Exception e) {
                    log.warn("[WeatherCollectJobConfig] 사용자 {}의 날씨 수집 실패: {}", u.getId(), e.getMessage());
                    fail++;
                }
            }
            log.info("[WeatherCollectJobConfig] 날씨 배치 작업 완료. 성공 {}건, 실패 {}건", success, fail);
            return RepeatStatus.FINISHED;
        };
    }
}