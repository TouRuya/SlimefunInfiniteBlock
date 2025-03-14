package net.touruya.infiniteblock.core.managers;

import lombok.Getter;
import net.touruya.infiniteblock.core.listeners.BlockListener;
import net.touruya.infiniteblock.implementation.InfiniteBlocks;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ListenerManager {
    private InfiniteBlocks plugin;
    private @NotNull List<Listener> listeners = new ArrayList<>();

    public ListenerManager(InfiniteBlocks plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        listeners.add(new BlockListener(plugin));
        registerListeners();
    }

    public void registerListeners() {
        for (Listener listener : listeners) {
            Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }
}
