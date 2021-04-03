package chat.tidy.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class MemorySafeAsyncPlayerChatEvent {

    private final boolean asynchronous;
    private final UUID authorUID;
    private final String message;
    private final Set<UUID> recipients;

    MemorySafeAsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
        this.asynchronous = event.isAsynchronous();
        this.authorUID = event.getPlayer().getUniqueId();
        this.message = event.getMessage();
        this.recipients = event.getRecipients().stream().map(Player::getUniqueId).collect(Collectors.toSet());
    }

    public boolean isAsynchronous() {
        return asynchronous;
    }

    public UUID getAuthorUID() {
        return authorUID;
    }

    public String getMessage() {
        return message;
    }

    public Set<UUID> getRecipients() {
        return recipients;
    }
}
