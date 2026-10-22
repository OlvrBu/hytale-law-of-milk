package io.github.obut.lawofmilk;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import io.github.obut.lawofmilk.commands.ExampleCommand;
import io.github.obut.lawofmilk.config.ExampleConfig;
import io.github.obut.lawofmilk.events.ExampleEvent;
import io.github.obut.lawofmilk.gnomeblock.GnomeBlock;

import javax.annotation.Nonnull;

public class LawOfMilkPlugin extends JavaPlugin {

    private static Config<ExampleConfig> config = null;

    public LawOfMilkPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        config = this.withConfig("example_config", ExampleConfig.CODEC);
    }

    @Override
    protected void setup() {
        config.save();
        GnomeBlock.register();
        this.getCommandRegistry().registerCommand(new ExampleCommand("example", "An example command"));
        if (getConfig().get().isEnabledWelcomeMessage()) {
            this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, ExampleEvent::onPlayerReady);
        }
    }

    public static Config<ExampleConfig> getConfig() {
        return config;
    }
}