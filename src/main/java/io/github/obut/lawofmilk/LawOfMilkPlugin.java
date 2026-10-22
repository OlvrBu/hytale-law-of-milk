package io.github.obut.lawofmilk;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import io.github.obut.lawofmilk.gnomeblock.GnomeBlock;
import io.github.obut.lawofmilk.superhatchet.SuperHatchet;

import javax.annotation.Nonnull;

/** Entry point. Each feature lives in its own subpackage and registers itself here. */
public class LawOfMilkPlugin extends JavaPlugin {

    public LawOfMilkPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        GnomeBlock.register(this);
        SuperHatchet.register(this);
        getCommandRegistry().registerCommand(new LawOfMilkCommand());
    }
}
