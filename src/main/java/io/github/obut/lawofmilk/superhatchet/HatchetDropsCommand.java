package io.github.obut.lawofmilk.superhatchet;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

/** {@code /lawofmilk hatchetdrops}: toggles whether the super hatchet's area drops items, until the server restarts. */
public class HatchetDropsCommand extends CommandBase {

    public HatchetDropsCommand() {
        super("hatchetdrops", "Toggle item drops for the super hatchet's area (not saved)");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        boolean now = !SuperHatchet.areaDrops;
        SuperHatchet.areaDrops = now;
        context.sendMessage(Message.raw("Super hatchet area drops: " + (now ? "on" : "off")));
    }
}
