package io.github.obut.lawofmilk.hostilecrate;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

/**
 * The hostile capture crate (item {@code Tool_Capture_Crate_Hostile}, defined in the asset pack): a
 * capture crate for hostile creatures. It only catches them once they are weakened, and it breaks
 * when the creature is released.
 */
public final class HostileCrate {

    private HostileCrate() {
    }

    /**
     * Makes the interaction type usable in asset JSON. Must run in the plugin's setup(), before the
     * server loads the item that refers to it.
     */
    public static void register(JavaPlugin plugin) {
        plugin.getCodecRegistry(Interaction.CODEC)
                .register(HostileCrateInteraction.TYPE, HostileCrateInteraction.class, HostileCrateInteraction.CODEC);
    }
}
