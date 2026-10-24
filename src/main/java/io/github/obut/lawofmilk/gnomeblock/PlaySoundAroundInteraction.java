package io.github.obut.lawofmilk.gnomeblock;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Block interaction that plays a sound at a random point around the block, e.g. as a block's "Use"
 * interaction: {@code {"Type": "LawOfMilk_PlaySoundAround", "SoundEventId": "...", "OffsetRadius": 6}}.
 *
 * <p>With {@code "Chance"} below 1 the sound plays only with that probability; otherwise the
 * interaction fails, so the JSON's {@code "Failed"} branch runs instead (e.g. a default sound).
 *
 * <p>The server creates instances from the block JSON via {@link #CODEC}. {@link SimpleBlockInteraction}
 * resolves the target block and checks the player is close enough before calling {@link #interactWithBlock}.
 */
public class PlaySoundAroundInteraction extends SimpleBlockInteraction {

    public static final String TYPE = "LawOfMilk_PlaySoundAround";

    public static final BuilderCodec<PlaySoundAroundInteraction> CODEC = BuilderCodec.builder(
                    PlaySoundAroundInteraction.class, PlaySoundAroundInteraction::new, SimpleBlockInteraction.CODEC)
            .documentation("Plays a sound at a random point around the block.")
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
                    new KeyedCodec<>("OffsetRadius", Codec.DOUBLE),
                    (o, v) -> o.offsetRadius = v,
                    o -> o.offsetRadius,
                    (o, p) -> o.offsetRadius = p.offsetRadius
            )
            .addValidator(Validators.range(0.0, 64.0))
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
    private double offsetRadius = 0.0;
    private double chance = 1.0;

    // Runs on the server, on the world thread.
    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                     @Nonnull InteractionType type, @Nonnull InteractionContext context,
                                     @Nullable ItemStack itemInHand, @Nonnull Vector3i targetBlock,
                                     @Nonnull CooldownHandler cooldownHandler) {
        // Rolled on the server only; the client doesn't predict it, so a miss arrives as a
        // server-side failure and the client follows the "Failed" branch.
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        ScreamSound.playAround(world, soundEventId, targetBlock.x, targetBlock.y, targetBlock.z, offsetRadius);
    }

    // Client-side prediction hook. Nothing to predict: the server sends the sound.
    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context,
                                             @Nullable ItemStack itemInHand, @Nonnull World world,
                                             @Nonnull Vector3i targetBlock) {
    }
}
