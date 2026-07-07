package com.azthera.ecocore.jobs;
 
import com.azthera.ecocore.bootstrap.Module;
import com.azthera.ecocore.config.JobsConfig;
import com.azthera.ecocore.data.model.JobData;
import com.azthera.ecocore.data.repository.JobDataRepository;
import com.azthera.ecocore.integration.EcoScheduledTask;
import com.azthera.ecocore.integration.SchedulerAdapter;
 
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
 
/**
 * Lifecycle owner of the Jobs module. Loads each online player's job data on
 * join, exposes lookups for the Jobs GUI/command, and runs a periodic check
 * for daily bonus window transitions to notify listeners if needed in the future.
 */
public final class JobManager implements Module {
 
    private final JobDataRepository jobDataRepository;
    private final BonusScheduler bonusScheduler;
    private final SchedulerAdapter schedulerAdapter;
    private JobsConfig jobsConfig;
    private final Logger logger;
    private EcoScheduledTask bonusCheckTask;
    private boolean enabled;
 
    public JobManager(JobDataRepository jobDataRepository, BonusScheduler bonusScheduler,
                       SchedulerAdapter schedulerAdapter, JobsConfig jobsConfig, Logger logger) {
        this.jobDataRepository = jobDataRepository;
        this.bonusScheduler = bonusScheduler;
        this.schedulerAdapter = schedulerAdapter;
        this.jobsConfig = jobsConfig;
        this.logger = logger;
    }
 
    @Override
    public String getName() {
        return "jobs";
    }
 
    @Override
    public void enable() {
        this.enabled = true;
        bonusScheduler.configure(jobsConfig.getDailyResetHourOfDay());
        bonusCheckTask = schedulerAdapter.runAsyncRepeating(this::checkDailyReset, 20L * 60L, 20L * 60L);
        logger.info("Jobs module enabled.");
    }
 
    @Override
    public void disable() {
        this.enabled = false;
        if (bonusCheckTask != null && !bonusCheckTask.isCancelled()) {
            bonusCheckTask.cancel();
        }
    }
 
    @Override
    public void reload() {
        disable();
        enable();
    }
 
    @Override
    public boolean isEnabled() {
        return enabled;
    }
 
    private void checkDailyReset() {
        if (bonusScheduler.hasDailyResetOccurredSinceLastCheck()) {
            logger.info("Daily job bonus window has reset.");
        }
    }
 
    public void loadPlayerJobs(UUID playerId) {
        jobDataRepository.loadForPlayerAsync(playerId);
    }
 
    public void unloadPlayerJobs(UUID playerId) {
        List<String> jobIds = List.of(
            JobType.MINING.name(), JobType.WOODCUTTING.name(), JobType.FISHING.name(),
            JobType.FARMING.name(), JobType.HUNTING.name(), JobType.EXPLORING.name(), JobType.BUILDER.name()
        );
        jobDataRepository.invalidatePlayer(playerId, jobIds);
    }
 
    public JobData getOrDefault(UUID playerId, JobType jobType) {
        return jobDataRepository.getCached(playerId, jobType.name())
            .orElseGet(() -> new JobData(playerId, jobType.name(), 1, 0.0, 0, 0L, 0L));
    }
 
    public Set<JobType> getEnabledJobTypes() {
        return java.util.Arrays.stream(JobType.values())
            .filter(type -> {
                JobsConfig.JobDefinition definition = jobsConfig.getJobDefinitions().get(type.name());
                return definition != null && definition.enabled();
            })
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
 
    public void updateJobsConfig(JobsConfig jobsConfig) {
        this.jobsConfig = jobsConfig;
        bonusScheduler.configure(jobsConfig.getDailyResetHourOfDay());
    }
}
