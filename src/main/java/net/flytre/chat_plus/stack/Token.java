package net.flytre.chat_plus.stack;

import net.minecraft.text.MutableText;

public interface Token {

    record TextToken(MutableText text) implements Token {
    }

    record MatchToken(ExactMatch match) implements Token {

    }


    record ExactMatch(StackChatBuilder.PatternData data, boolean start, String exactMatch) {}
}
