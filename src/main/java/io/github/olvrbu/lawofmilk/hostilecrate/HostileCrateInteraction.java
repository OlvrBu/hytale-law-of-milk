package io.github.olvrbu.lawofmilk.hostilecrate;

import com.hypixel.hytale.builtin.adventure.farming.interactions.UseCaptureCrateInteraction;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.metadata.CapturedNPCMetadata;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The vanilla capture crate interaction ({@code UseCaptureCrate}) with two changes:
 * <ul>
 *   <li>Catching only works on NPCs whose default attitude towards players is {@code Hostile}, and
 *       only once their health is at or below {@code "MaxHealthFraction"} (default 0.25). Otherwise
 *       the interaction fails and the player gets a chat message.</li>
 *   <li>Releasing the creature uses up the crate.</li>
 * </ul>
 * {@code "AcceptedNpcGroups"} still applies on top (vanilla check), so the item JSON can use a
 * group that matches every role and leave the hostility decision to this class.
 */
public class HostileCrateInteraction extends UseCaptureCrateInteraction {

    public static final String TYPE = "LawOfMilk_UseHostileCrate";

    // Metadata key under which the vanilla interaction stores the caught NPC on the item.
    private static final String CAPTURED_KEY = "CapturedEntity";

    // Built on top of the vanilla codec, so AcceptedNpcGroups, FullIcon and the vanilla
    // afterDecode (group id -> index) are inherited.
    public static final BuilderCodec<HostileCrateInteraction> CODEC = BuilderCodec.builder(
                    HostileCrateInteraction.class, HostileCrateInteraction::new, UseCaptureCrateInteraction.CODEC)
            .documentation("Capture crate for weakened hostile NPCs; breaks on release.")
            .<Double>appendInherited(
                    new KeyedCodec<>("MaxHealthFraction", Codec.DOUBLE),
                    (o, v) -> o.maxHealthFraction = v,
                    o -> o.maxHealthFraction,
                    (o, p) -> o.maxHealthFraction = p.maxHealthFraction
            )
            .addValidator(Validators.range(0.0, 1.0))
            .add()
            .build();

    private double maxHealthFraction = 0.25;

    // Catching: the vanilla tick0 catches the target entity when the crate is empty.
    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type, @Nonnull InteractionContext context,
                         @Nonnull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        ItemStack item = context.getHeldItem();
        Ref<EntityStore> target = context.getTargetEntity();
        if (commandBuffer != null && item != null && target != null && isEmpty(item)
                && commandBuffer.getComponent(target, NPCEntity.getComponentType()) != null) {
            Message refusal = refusal(commandBuffer, target);
            if (refusal != null) {
                PlayerRef player = commandBuffer.getComponent(context.getEntity(), PlayerRef.getComponentType());
                if (player != null) {
                    player.sendMessage(refusal);
                }
                context.getState().state = InteractionState.Failed;
                // What SimpleInteraction.tick0 does on failure: continue with the JSON's "Failed" branch.
                // (Java can't call the grandparent's method directly, and the parent's would catch.)
                if (context.hasLabels()) {
                    context.jump(context.getLabel(0));
                }
                return;
            }
        }
        super.tick0(firstRun, time, type, context, cooldownHandler);
    }

    // Releasing: the vanilla code spawns the NPC at the clicked block and empties the crate.
    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                     @Nonnull InteractionType type, @Nonnull InteractionContext context,
                                     @Nullable ItemStack itemInHand, @Nonnull Vector3i targetBlock,
                                     @Nonnull CooldownHandler cooldownHandler) {
        ItemStack item = context.getHeldItem();
        boolean wasFull = item != null && !isEmpty(item);
        super.interactWithBlock(world, commandBuffer, type, context, itemInHand, targetBlock, cooldownHandler);
        if (!wasFull || context.getState().state == InteractionState.Failed) {
            return;
        }
        InventoryComponent.Hotbar hotbar = commandBuffer.getComponent(context.getEntity(), InventoryComponent.Hotbar.getComponentType());
        if (hotbar != null) {
            hotbar.getInventory().removeItemStackFromSlot(hotbar.getActiveSlot());
        }
    }

    /** Why the target can't be caught, or null if it can. */
    @Nullable
    private Message refusal(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Ref<EntityStore> target) {
        // WorldSupport holds the role's resolved "DefaultPlayerAttitude" (the fallback the NPC
        // attitude system uses for players).
        WorldSupport worldSupport = commandBuffer.getComponent(target, WorldSupport.getComponentType());
        if (worldSupport == null || worldSupport.getDefaultPlayerAttitude() != Attitude.HOSTILE) {
            return Message.translation("lawofmilk.hostileCrate.notHostile");
        }
        EntityStatMap stats = commandBuffer.getComponent(target, EntityStatMap.getComponentType());
        EntityStatValue health = stats == null ? null : stats.get(DefaultEntityStatTypes.getHealth());
        if (health != null && !CaptureRule.isWeakEnough(health.get(), health.getMax(), maxHealthFraction)) {
            return Message.translation("lawofmilk.hostileCrate.tooHealthy")
                    .param("percent", CaptureRule.percent(maxHealthFraction));
        }
        return null;
    }

    private static boolean isEmpty(@Nonnull ItemStack item) {
        return item.getFromMetadataOrNull(CAPTURED_KEY, CapturedNPCMetadata.CODEC) == null;
    }
}
