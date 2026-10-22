package io.github.obut.lawofmilk.gnomeblock;

import com.hypixel.hytale.server.core.asset.type.blocktick.config.RandomTickProcedure;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

/**
 * The suspicious plushies: copies of the Kweebec and Goblin plushies (items
 * {@code Deco_Kweebec_Plush_Suspicious} and {@code Deco_Goblin_Plush_Suspicious}, defined in the
 * asset pack) that now and then play the gnome scream somewhere around them, and play it when used.
 */
public final class GnomeBlock {

    private GnomeBlock() {
    }

    /**
     * Makes the procedure and interaction types usable in asset JSON. Must run in the plugin's
     * setup(), before the server loads the block types that refer to them. The plugin's codec
     * registry removes them again when the plugin shuts down.
     */
    public static void register(JavaPlugin plugin) {
        plugin.getCodecRegistry(RandomTickProcedure.CODEC)
                .register(RandomSoundProcedure.TYPE, RandomSoundProcedure.class, RandomSoundProcedure.CODEC);
        plugin.getCodecRegistry(Interaction.CODEC)
                .register(PlaySoundAroundInteraction.TYPE, PlaySoundAroundInteraction.class, PlaySoundAroundInteraction.CODEC);
    }
}
