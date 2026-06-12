package net.Indyuce.mmoitems.command.mmoitems.debug;

import io.lumine.mythic.lib.command.CommandTreeNode;

public class DebugCommandTreeNode extends CommandTreeNode {
    public DebugCommandTreeNode(CommandTreeNode parent) {
        super(parent, "debug");

        addChild(new CheckStatCommandTreeNode(this));
        addChild(new CheckAttributeCommandTreeNode(this));
        addChild(new CheckTagCommandTreeNode(this));
        addChild(new SetTagCommandTreeNode(this));
        addChild(new CheckTagsCommandTreeNode(this));
        addChild(new InfoCommandTreeNode(this)); // backwards compat. moved to ml
        addChild(new HealCommandTreeNode(this));
        addChild(new TestCommandTreeNode(this));
    }
}
