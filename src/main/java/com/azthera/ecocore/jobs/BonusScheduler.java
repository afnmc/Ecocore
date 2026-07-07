package com.azthera.ecocore.jobs;
 
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.atomic.AtomicReference;
 
/**
 * Tracks whether the Daily Bonus and Weekly Bonus windows are currently
 * active, so {@code JobRewardCalculator} can apply the configured
 * multipliers. The daily bonus is active for the first hour after each
 * day's reset time; the weekly bonus is active all weekend (Sat/Sun),
 * matching a simple, predictable schedule server owners can communicate to players.
 */
public final class BonusScheduler {
 
    private static final int DAILY_BONUS_DURATION_HOURS = 1;
 
    private final ZoneId zoneId = ZoneId.systemDefault();
    private final AtomicReference<LocalDate> lastDailyResetDate = new AtomicReference<>(LocalDate.now());
    private int dailyResetHour = 0;
 
    public void configure(long dailyResetHourOfDay) {
        this.dailyResetHour = (int) Math.max(0, Math.min(23, dailyResetHourOfDay));
    }
 
    public boolean isDailyBonusActive() {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime todayReset = now.toLocalDate().atTime(LocalTime.of(dailyResetHour, 0)).atZone(zoneId);
        ZonedDateTime bonusWindowEnd = todayReset.plusHours(DAILY_BONUS_DURATION_HOURS);
 
        if (now.isBefore(todayReset)) {
            ZonedDateTime yesterdayReset = todayReset.minusDays(1);
            ZonedDateTime yesterdayWindowEnd = yesterdayReset.plusHours(DAILY_BONUS_DURATION_HOURS);
            return !now.isBefore(yesterdayReset) && now.isBefore(yesterdayWindowEnd);
        }
 
        return !now.isBefore(todayReset) && now.isBefore(bonusWindowEnd);
    }
 
    public boolean isWeeklyBonusActive() {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        return now.getDayOfWeek().getValue() >= 6;
    }
 
    public boolean hasDailyResetOccurredSinceLastCheck() {
        LocalDate today = LocalDate.now(zoneId);
        LocalDate previous = lastDailyResetDate.getAndSet(today);
        return !previous.equals(today);
    }
 
    public ZonedDateTime getNextWeeklyResetTime() {
        return ZonedDateTime.now(zoneId)
            .with(TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))
            .toLocalDate().atStartOfDay(zoneId);
    }
}
