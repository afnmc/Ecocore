package com.azthera.ecocore.jobs;
 
import com.azthera.ecocore.config.JobsConfig;
import com.azthera.ecocore.data.model.JobData;
import com.azthera.ecocore.data.repository.JobDataRepository;
import com.azthera.ecocore.util.Result;
 
/**
 * Handles the prestige action: resets a job's level/XP back to 1 in
 * exchange for permanently incrementing its prestige tier (and therefore
 * its reward multiplier via {@code JobRewardCalculator}), up to the
 * configured maximum prestige tier. Requires the job to be at max level
 * before prestiging, matching typical prestige-system expectations.
 */
public final class PrestigeManager {
 
    private final JobDataRepository jobDataRepository;
    private JobsConfig jobsConfig;
 
    public PrestigeManager(JobDataRepository jobDataRepository, JobsConfig jobsConfig) {
        this.jobDataRepository = jobDataRepository;
        this.jobsConfig = jobsConfig;
    }
 
    public void reload(JobsConfig jobsConfig) {
        this.jobsConfig = jobsConfig;
    }
 
    public Result<Integer> prestige(JobData jobData) {
        if (jobData.getLevel() < jobsConfig.getMaxLevel()) {
            return Result.failure("Job harus mencapai level maksimum sebelum bisa prestige.");
        }
        if (jobData.getPrestigeTier() >= jobsConfig.getMaxPrestige()) {
            return Result.failure("Job ini sudah mencapai prestige maksimum.");
        }
 
        int newTier = jobData.getPrestigeTier() + 1;
        jobData.setPrestigeTier(newTier);
        jobData.setLevel(1);
        jobData.setExperience(0.0);
        jobDataRepository.save(jobData);
 
        return Result.success(newTier);
    }
 
    public boolean canPrestige(JobData jobData) {
        return jobData.getLevel() >= jobsConfig.getMaxLevel() && jobData.getPrestigeTier() < jobsConfig.getMaxPrestige();
    }
 
    public int getMaxPrestige() {
        return jobsConfig.getMaxPrestige();
    }
}
