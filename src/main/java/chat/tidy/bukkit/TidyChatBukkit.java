package chat.tidy.bukkit;

import chat.tidy.TidyChat;
import org.bukkit.plugin.java.JavaPlugin;

public final class TidyChatBukkit extends JavaPlugin {

    public static TidyChatBukkit INSTANCE;

    private TidyChat tidyChat = null;

    public TidyChatBukkit() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.saveDefaultConfig();
        tidyChat = TidyChat.getInstance();
        UniversalListener universalListener = new UniversalListener(
                tidyChat,
                getConfig().getBoolean("sendMessageIfDisconnected"),
                getConfig().getConfigurationSection("messages")
        );
        getServer().getPluginManager().registerEvents(universalListener, this);
        tidyChat.getEventManager().registerListener(universalListener);
        tidyChat.setLoggerConsumer(message -> getLogger().info(message));
        String bearer = getConfig().getString("tidychatBearer");
        if (bearer != null && !bearer.isEmpty()) {
            tidyChat.setBearer(bearer);
        }
        tidyChat.getSocketClient().connect();
    }

    @Override
    public void onDisable() {
        if (tidyChat != null) {
            tidyChat.getSocketClient().close();
        }
    }
}
