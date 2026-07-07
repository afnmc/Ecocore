package com.azthera.ecocore.market;
 
import com.azthera.ecocore.config.MarketConfig;
import com.azthera.ecocore.integration.SchedulerAdapter;
import com.azthera.ecocore.util.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
 
/**
 * Broadcasts market event start/end announcements to all online players via
 * the server's Adventure audience, respecting the news.enabled toggle in
 * modules/market.yml. All broadcasts are dispatched on the main/global thread.
 */
public final class ServerNewsBroadcaster {
 
    private final MessageService messageService;
    private final SchedulerAdapter schedulerAdapter;
    private MarketConfig marketConfig;
 
    public ServerNewsBroadcaster(MessageService messageService, SchedulerAdapter schedulerAdapter,
                                  MarketConfig marketConfig) {
        this.messageService = messageService;
        this.schedulerAdapter = schedulerAdapter;
        this.marketConfig = marketConfig;
    }
 
    public void reload(MarketConfig marketConfig) {
        this.marketConfig = marketConfig;
    }
 
    public void announceEventStarted(MarketEventType eventType) {
        if (!marketConfig.isNewsBroadcastEnabled()) {
            return;
        }
        Component message = messageService.render("market.event-started",
            MessageService.placeholder("event", formatEventName(eventType)));
        schedulerAdapter.runSync(() -> Bukkit.getServer().sendMessage(message));
    }
 
    public void announceEventEnded(MarketEventType eventType) {
        if (!marketConfig.isNewsBroadcastEnabled()) {
            return;
        }
        Component message = messageService.render("market.event-ended",
            MessageService.placeholder("event", formatEventName(eventType)));
        schedulerAdapter.runSync(() -> Bukkit.getServer().sendMessage(message));
    }
 
    private String formatEventName(MarketEventType eventType) {
        String[] words = eventType.name().split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        return builder.toString();
    }
}
