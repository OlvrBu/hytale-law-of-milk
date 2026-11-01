package io.github.olvrbu.lawofmilk.superapple;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The apple's primary (left-click) interaction, set in our override of {@code Plant_Fruit_Apple}.
 * If the super apple is active, removes the clicked block and every block of its type in a cone
 * from the player's body along their view (see {@link SuperApple}) at once (no damage, cracks or
 * drops; any block type). Otherwise it
 * fails, so the JSON's {@code "Failed"} branch continues with the vanilla {@code Block_Primary}
 * chain and the apple punches as usual.
 *
 * <p>This runs before the server's gather and quality checks, which only happen inside the
 * vanilla BreakBlock interaction.
 */
public class SuperAppleInteraction extends SimpleBlockInteraction {

    public static final String TYPE = "LawOfMilk_SuperApple";

    // No settings of its own; inherits the parent's (e.g. "UseLatestTarget", "Next", "Failed").
    public static final BuilderCodec<SuperAppleInteraction> CODEC = BuilderCodec.builder(
                    SuperAppleInteraction.class, SuperAppleInteraction::new, SimpleBlockInteraction.CODEC)
            .documentation("Removes the clicked block and all blocks of its type around it if the super apple is active; fails otherwise.")
            .build();

    // Runs on the server, on the world thread. Not called for air: SimpleBlockInteraction fails then.
    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                     @Nonnull InteractionType type, @Nonnull InteractionContext context,
                                     @Nullable ItemStack itemInHand, @Nonnull Vector3i targetBlock,
                                     @Nonnull CooldownHandler cooldownHandler) {
        if (!SuperApple.isActive(itemInHand, commandBuffer, context.getEntity())) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        BlockType blockType = blockTypeAt(world, targetBlock);
        if (blockType == null || blockType == BlockType.EMPTY) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        Cone cone = viewCone(commandBuffer, context.getEntity());
        if (cone == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        Vector3i clicked = new Vector3i(targetBlock);
        // The interaction runs inside the entity store's tick; change blocks in a separate task.
        world.execute(() -> AreaRemover.removeArea(world, clicked, blockType, cone, SuperApple.logger()));
    }

    /** The cone from the player's body along their view direction, or null if the player has no position or head rotation. */
    @Nullable
    private static Cone viewCone(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> player) {
        TransformComponent transform = commandBuffer.getComponent(player, TransformComponent.getComponentType());
        HeadRotation head = commandBuffer.getComponent(player, HeadRotation.getComponentType());
        if (transform == null || head == null) {
            return null;
        }
        // The position is at the feet; the cone's tip runs from there up by the player's height.
        Vector3d feet = transform.getPosition();
        Vector3d look = head.getDirection();
        if (look.lengthSquared() == 0) {
            return null;
        }
        return new Cone(feet.x, feet.y, feet.z, SuperApple.TIP_HEIGHT, look.x, look.y, look.z,
                SuperApple.LENGTH, SuperApple.HALF_ANGLE_DEGREES);
    }

    /** The block type at {@code pos}, looked up the same way {@link SimpleBlockInteraction} does. */
    @Nullable
    private static BlockType blockTypeAt(World world, Vector3i pos) {
        ChunkStore chunks = world.getChunkStore();
        Ref<ChunkStore> section = chunks.getChunkSectionReferenceAtBlock(pos.x, pos.y, pos.z);
        if (section == null || !section.isValid()) {
            return null;
        }
        BlockSection blocks = chunks.getStore().getComponent(section, BlockSection.getComponentType());
        return blocks == null ? null : BlockType.getAssetMap().getAsset(blocks.get(pos.x, pos.y, pos.z));
    }

    // Client-side prediction hook. Nothing to predict: the server sends the block changes.
    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context,
                                             @Nullable ItemStack itemInHand, @Nonnull World world,
                                             @Nonnull Vector3i targetBlock) {
    }
}
