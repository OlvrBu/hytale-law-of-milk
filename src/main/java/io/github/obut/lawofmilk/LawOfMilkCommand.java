package io.github.obut.lawofmilk;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import io.github.obut.lawofmilk.superhatchet.HatchetDropsCommand;
import io.github.obut.lawofmilk.superhatchet.HatchetRadiusCommand;

/** {@code /lawofmilk}: parent of the features' (operator-only) commands. */
public class LawOfMilkCommand extends AbstractCommandCollection {

    public LawOfMilkCommand() {
        super("lawofmilk", "Law Of Milk commands");
        addSubCommand(new HatchetRadiusCommand());
        addSubCommand(new HatchetDropsCommand());
    }
}
