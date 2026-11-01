package io.github.olvrbu.lawofmilk;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import io.github.olvrbu.lawofmilk.gnomeblock.GnomeBlock;
import io.github.olvrbu.lawofmilk.growingchest.GrowingChest;
import io.github.olvrbu.lawofmilk.hostilecrate.HostileCrate;
import io.github.olvrbu.lawofmilk.junglecandle.JungleCandle;
import io.github.olvrbu.lawofmilk.superapple.SuperApple;

import javax.annotation.Nonnull;

/** Entry point. Each feature lives in its own subpackage and registers itself here. */
public class LawOfMilkPlugin extends JavaPlugin {

    public LawOfMilkPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        GnomeBlock.register(this);
        SuperApple.register(this);
        JungleCandle.register(this);
        HostileCrate.register(this);
        GrowingChest.register(this);
    }
}
