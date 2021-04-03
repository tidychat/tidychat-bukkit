package chat.tidy.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TidyChatAsyncPlayerChatEvent extends AsyncPlayerChatEvent {

    public TidyChatAsyncPlayerChatEvent(boolean async, Player who, String message, Set<Player> players) {
        super(async, who, message, players);
    }

    public TidyChatAsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
        this(event.isAsynchronous(), event.getPlayer(), event.getMessage(), event.getRecipients());
    }

    public static TidyChatAsyncPlayerChatEvent fromMemorySafeAsyncPlayerChatEvent(MemorySafeAsyncPlayerChatEvent data) {
        Player player = Bukkit.getPlayer(data.getAuthorUID());
        if (player == null || !player.isOnline()) return null;
        return new TidyChatAsyncPlayerChatEvent(data.isAsynchronous(), player, data.getMessage(), data.getRecipients()
                .stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(Player::isOnline)
                .collect(Collectors.toSet())
        );
    }
}
