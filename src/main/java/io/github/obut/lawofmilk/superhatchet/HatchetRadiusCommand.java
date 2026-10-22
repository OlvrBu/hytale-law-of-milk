package io.github.obut.lawofmilk.superhatchet;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

/** {@code /lawofmilk hatchetradius <blocks>}: sets the super hatchet's area radius until the server restarts. */
public class HatchetRadiusCommand extends CommandBase {

    private final RequiredArg<Integer> radiusArg = withRequiredArg("blocks", "Radius in blocks", ArgTypes.INTEGER);

    public HatchetRadiusCommand() {
        super("hatchetradius", "Set the super hatchet's area radius (not saved)");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        int radius = radiusArg.get(context);
        if (radius < 0 || radius > SuperHatchet.MAX_RADIUS) {
            context.sendMessage(Message.raw("Radius must be between 0 and " + SuperHatchet.MAX_RADIUS + "."));
            return;
        }
        int old = SuperHatchet.radius;
        SuperHatchet.radius = radius;
        context.sendMessage(Message.raw("Super hatchet radius: " + old + " -> " + radius));
    }
}
