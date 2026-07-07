package com.azthera.ecocore.util;
 
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
 
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
 
/**
 * Resolves message keys (from messages.yml) into Adventure Components via MiniMessage.
 * Deliberately decoupled from the config package (it only receives a flat key->template
 * map on reload) so it has no forward dependency on {@code MessagesConfig}.
 */
public final class MessageService {
 
    private final MiniMessage miniMessage;
    private final Map<String, String> templates;
    private final String prefix;
 
    public MessageService(String prefix) {
        this.miniMessage = MiniMessage.miniMessage();
        this.templates = new HashMap<>();
        this.prefix = Objects.requireNonNullElse(prefix, "");
    }
 
    public void reload(Map<String, String> newTemplates) {
        templates.clear();
        templates.putAll(newTemplates);
    }
 
    public Component render(String key, TagResolver... resolvers) {
        String template = templates.getOrDefault(key, key);
        return miniMessage.deserialize(prefix + template, resolvers);
    }
 
    public Component renderNoPrefix(String key, TagResolver... resolvers) {
        String template = templates.getOrDefault(key, key);
        return miniMessage.deserialize(template, resolvers);
    }
 
    public Component renderRaw(String rawTemplate, TagResolver... resolvers) {
        return miniMessage.deserialize(rawTemplate, resolvers);
    }
 
    public void send(Audience audience, String key, TagResolver... resolvers) {
        audience.sendMessage(render(key, resolvers));
    }
 
    public String stripTags(String key) {
        String template = templates.getOrDefault(key, key);
        return PlainTextComponentSerializer.plainText().serialize(miniMessage.deserialize(template));
    }
 
    public static TagResolver placeholder(String key, String value) {
        return Placeholder.parsed(key, value);
    }
 
    public static TagResolver placeholderComponent(String key, Component value) {
        return Placeholder.component(key, value);
    }
 
    public boolean hasTemplate(String key) {
        return templates.containsKey(key);
    }
}
