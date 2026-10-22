package io.github.obut.lawofmilk.gnomeblock;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.blocktick.config.RandomTickProcedure;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Random tick procedure that plays a sound at the block with a given chance per random tick.
 * Used in a block type's JSON as
 * {@code "RandomTickProcedure": {"Type": "LawOfMilk_RandomSound", "SoundEventId": "...", "Chance": 0.05}}.
 *
 * <p>Each loaded block gets on average one random tick every 8192 server ticks (about 4.6 minutes
 * at 30 TPS; see the built-in RandomTickSystem), so the average time between sounds is roughly
 * 4.6 minutes / Chance.
 */
public class RandomSoundProcedure implements RandomTickProcedure {

    public static final String TYPE = "LawOfMilk_RandomSound";

    // appendInherited (vs. append) lets a child block type that sets "Parent" inherit the values.
    public static final BuilderCodec<RandomSoundProcedure> CODEC = BuilderCodec.builder(RandomSoundProcedure.class, RandomSoundProcedure::new)
            .appendInherited(
                    new KeyedCodec<>("SoundEventId", Codec.STRING),
                    (o, v) -> o.soundEventId = v,
                    o -> o.soundEventId,
                    (o, p) -> o.soundEventId = p.soundEventId
            )
            .addValidator(Validators.nonNull())
            .addValidator(SoundEvent.VALIDATOR_CACHE.getValidator())
            .add()
            .<Double>appendInherited(
                    new KeyedCodec<>("Chance", Codec.DOUBLE),
                    (o, v) -> o.chance = v,
                    o -> o.chance,
                    (o, p) -> o.chance = p.chance
            )
            .addValidator(Validators.range(0.0, 1.0))
            .add()
            .build();

    private String soundEventId;
    private double chance = 1.0;

    @Override
    public void onRandomTick(Store<ChunkStore> store, CommandBuffer<ChunkStore> commandBuffer, BlockSection blockSection,
                             int worldX, int worldY, int worldZ, int blockId, BlockType blockType) {
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        int soundEventIndex = SoundEvent.getAssetMap().getIndex(soundEventId);
        if (soundEventIndex == Integer.MIN_VALUE) {
            return;
        }
        // Random ticks run inside the chunk store's tick, but finding the players who can hear the
        // sound needs the entity store, so play it as a separate task on the same world thread.
        World world = store.getExternalData().getWorld();
        world.execute(() -> SoundUtil.playSoundEvent3d(soundEventIndex, SoundCategory.SFX,
                worldX + 0.5, worldY + 0.5, worldZ + 0.5, world.getEntityStore().getStore()));
    }
}
