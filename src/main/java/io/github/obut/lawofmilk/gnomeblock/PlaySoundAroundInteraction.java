package io.github.obut.lawofmilk.gnomeblock;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
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

/**
 * Block interaction that plays a sound at a random point around the block, e.g. as a block's "Use"
 * interaction: {@code {"Type": "LawOfMilk_PlaySoundAround", "SoundEventId": "...", "OffsetRadius": 6}}.
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
            .build();

    private String soundEventId;
    private double offsetRadius = 0.0;

    // Runs on the server, on the world thread.
    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                     @Nonnull InteractionType type, @Nonnull InteractionContext context,
                                     @Nullable ItemStack itemInHand, @Nonnull Vector3i targetBlock,
                                     @Nonnull CooldownHandler cooldownHandler) {
        ScreamSound.playAround(world, soundEventId, targetBlock.x, targetBlock.y, targetBlock.z, offsetRadius);
    }

    // Client-side prediction hook. Nothing to predict: the server sends the sound.
    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context,
                                             @Nullable ItemStack itemInHand, @Nonnull World world,
                                             @Nonnull Vector3i targetBlock) {
    }
}
