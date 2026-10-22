package io.github.obut.lawofmilk.superapple;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

/** {@code /lawofmilk appleradius <blocks>}: sets the super apple's area radius until the server restarts. */
public class AppleRadiusCommand extends CommandBase {

    private final RequiredArg<Integer> radiusArg = withRequiredArg("blocks", "Radius in blocks", ArgTypes.INTEGER);

    public AppleRadiusCommand() {
        super("appleradius", "Set the super apple's area radius (not saved)");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        int radius = radiusArg.get(context);
        if (radius < 0 || radius > SuperApple.MAX_RADIUS) {
            context.sendMessage(Message.raw("Radius must be between 0 and " + SuperApple.MAX_RADIUS + "."));
            return;
        }
        int old = SuperApple.radius;
        SuperApple.radius = radius;
        context.sendMessage(Message.raw("Super apple radius: " + old + " -> " + radius));
    }
}
