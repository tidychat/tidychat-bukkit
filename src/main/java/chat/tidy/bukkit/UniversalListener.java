package chat.tidy.bukkit;

import chat.tidy.TidyChat;
import chat.tidy.event.ConnectionStateChangedEvent;
import chat.tidy.event.PacketInboundEvent;
import chat.tidy.event.PacketOutboundEvent;
import chat.tidy.message.ChatMessage;
import chat.tidy.message.StatusCode;
import chat.tidy.socket.ConnectionState;
import chat.tidy.socket.packet.CheckMessageInboundPacket;
import chat.tidy.socket.packet.CheckMessageOutboundPacket;
import chat.tidy.socket.packet.InboundPacket;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class UniversalListener implements chat.tidy.listener.Listener, org.bukkit.event.Listener {

    private final Cache<UUID, MemorySafeAsyncPlayerChatEvent> EVENT_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();

    private final TidyChat tidyChat;
    private final boolean sendMessageIfDisconnected;
    private final ConfigurationSection messages;

    UniversalListener(TidyChat tidyChat, boolean sendMessageIfDisconnected, ConfigurationSection messages) {
        this.tidyChat = tidyChat;
        this.sendMessageIfDisconnected = sendMessageIfDisconnected;
        this.messages = messages;
    }

    @org.bukkit.event.EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("tidychat.bypass")) return;
        if (tidyChat.getConnectionState() != ConnectionState.OPEN) {
            if (!sendMessageIfDisconnected) {
                String serverDisconnectedMessage = messages.getString("serverDisconnected");
                if (serverDisconnectedMessage != null && !serverDisconnectedMessage.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', serverDisconnectedMessage));
                }
                event.setCancelled(true);
            }
            return;
        }
        if (event instanceof TidyChatAsyncPlayerChatEvent) return;

        UUID messageUID = UUID.randomUUID();
        UUID authorUID = player.getUniqueId();
        String content = event.getMessage();
        ChatMessage chatMessage = new ChatMessage(messageUID, content);
        EVENT_CACHE.put(chatMessage.getMessageUID(), new MemorySafeAsyncPlayerChatEvent(event));
        tidyChat.getEventManager().callEvent(new PacketOutboundEvent(new CheckMessageOutboundPacket(authorUID, chatMessage)));

        event.setCancelled(true);
    }

    @chat.tidy.event.EventHandler
    public void onPacketInbound(PacketInboundEvent event) {
        InboundPacket inboundPacket = event.getPacket();
        if (inboundPacket instanceof CheckMessageInboundPacket) {
            CheckMessageInboundPacket checkMessageInboundPacket = (CheckMessageInboundPacket) inboundPacket;
            ChatMessage chatMessage = checkMessageInboundPacket.getChatMessage();
            UUID messageUID = chatMessage.getMessageUID();
            MemorySafeAsyncPlayerChatEvent data = EVENT_CACHE.getIfPresent(messageUID);
            if (data != null) {
                Player player = Bukkit.getServer().getPlayer(data.getAuthorUID());
                if (player != null && player.isOnline()) {
                    StatusCode[] statusCodes = chatMessage.getStatusCodes();
                    if (statusCodes != null && statusCodes.length > 0) {
                        String messageBlockedMessage = messages.getString("messageBlocked");
                        if (messageBlockedMessage != null && !messageBlockedMessage.isEmpty()) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', messageBlockedMessage));
                        }
                        return;
                    }
                    actuallySendMessage(data);
                    EVENT_CACHE.invalidate(messageUID);
                }
            }
        }
    }

    @chat.tidy.event.EventHandler
    public void onConnectionStateChanged(ConnectionStateChangedEvent event) {
        ConnectionState connectionState = event.getConnectionState();
        String message = messages.getString("tidychatState" + WordUtils.capitalize(connectionState.name().toLowerCase()));
        if (message != null && !message.isEmpty()) {
            Bukkit.getOnlinePlayers()
                    .stream()
                    .filter(player -> player.hasPermission("tidychat.notification"))
                    .forEach(player -> player.sendMessage(ChatColor.translateAlternateColorCodes('&', message)));
        }
    }

    private void actuallySendMessage(MemorySafeAsyncPlayerChatEvent data) {
        TidyChatAsyncPlayerChatEvent event = TidyChatAsyncPlayerChatEvent.fromMemorySafeAsyncPlayerChatEvent(data);
        if (event == null) return;

        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        String message = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
        ((CraftServer) Bukkit.getServer()).getServer().console.sendMessage(message);
        for (Player recipient : event.getRecipients()) {
            recipient.sendMessage(message);
        }
    }
}
