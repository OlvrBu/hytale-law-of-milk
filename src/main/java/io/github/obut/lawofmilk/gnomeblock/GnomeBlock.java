package io.github.obut.lawofmilk.gnomeblock;

import com.hypixel.hytale.server.core.asset.type.blocktick.config.RandomTickProcedure;

/**
 * The gnome-scream block: a copy of the Kweebec plushie (item {@code Deco_Kweebec_Plush_Screaming},
 * defined in the asset pack) that now and then plays the gnome scream.
 */
public final class GnomeBlock {

    private GnomeBlock() {
    }

    /** Must run in the plugin's setup(), before block types are loaded from the asset pack. */
    public static void register() {
        RandomTickProcedure.CODEC.register(RandomSoundProcedure.TYPE, RandomSoundProcedure.class, RandomSoundProcedure.CODEC);
    }
}
