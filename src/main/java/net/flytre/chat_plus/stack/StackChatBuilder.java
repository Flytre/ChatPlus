package net.flytre.chat_plus.stack;

import net.flytre.chat_plus.ChatPlus;
import net.flytre.chat_plus.ClassicChatBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StackChatBuilder {

    // Stores the list of patterns to identify and their functionality
    private static final List<PatternData> PATTERNS;

    static {
        PATTERNS = new ArrayList<>();
        PATTERNS.add(new PatternData(Pattern.compile("~~"), (style, match) -> style.withStrikethrough(true), null, StackChatBuilder::formattingPredicate));
        PATTERNS.add(new PatternData(Pattern.compile("\\[item]"), (style, match) -> style, (match, player) -> (MutableText) player.getMainHandStack().toHoverableText(), StackChatBuilder::hoverPredicate));
        PATTERNS.add(new PatternData(Pattern.compile("\\[(entity|me)]"), (style, match) -> style, (match, player) -> addEntityHover(player, (MutableText) player.getDisplayName()), StackChatBuilder::hoverPredicate));

        //Bold first because it takes precedence over italics that way
        PATTERNS.add(new PatternData(Pattern.compile("\\*{2}"), (style, match) -> style.withBold(true), null, StackChatBuilder::formattingPredicate));
        PATTERNS.add(new PatternData(Pattern.compile("\\*"), (style, match) -> style.withItalic(true), null, StackChatBuilder::formattingPredicate));

        Pattern color = Pattern.compile("&(([a-z-]+)|(#[0-9a-f]{6})) ",Pattern.CASE_INSENSITIVE);
        PATTERNS.add(new PatternData(color, Pattern.compile("/&"), (style, match) -> {
            Matcher matcher = color.matcher(match);
            return matcher.find() ? style.withColor(TextColor.parse(matcher.group(1))) : style;
        }, null, StackChatBuilder::colorPredicate));

        PATTERNS.add(new PatternData(Pattern.compile("\\$\\$"), (style, match) -> style.obfuscated(true), null, StackChatBuilder::formattingPredicate));

    }

    private static boolean colorPredicate(PlayerEntity player) {
        return ClassicChatBuilder.passesOpCheck(player, ChatPlus.CONFIG.getConfig().colorOp);
    }

    private static boolean formattingPredicate(PlayerEntity player) {
        return ClassicChatBuilder.passesOpCheck(player, ChatPlus.CONFIG.getConfig().formatOp);
    }

    private static boolean hoverPredicate(PlayerEntity player) {
        return ClassicChatBuilder.passesOpCheck(player, ChatPlus.CONFIG.getConfig().hoverOp) && !player.getMainHandStack().isEmpty();
    }


    private static void addIfNonEmpty(String str, Stack<Token> tokens) {
        if (str.length() > 0)
            tokens.add(new Token.TextToken(new LiteralText(str).setStyle(Style.EMPTY)));
    }


    /*
    Master logic method
     */
    public static Text fromString(String string, PlayerEntity player) {
        try {
            final Stack<Token> tokens = new Stack<>();
            String current = "";
            int index = 0;

            while (index <= string.length()) {

                //ADVANCED FORWARD MATCHING
                if (index < string.length()) {
                    Optional<PatternMatch> greedy = matches(current + string.charAt(index));
                    String str = greedy.map(match -> match.str).orElse(null);
                    while (greedy.isPresent() && greedy.get().match.end == current.length() + 1 && !(str.length() > 1 && str.charAt(str.length() - 2) == ' ')) {
                        current += string.charAt(index++);
                        if (index >= string.length())
                            break;
                        greedy = matches(current + string.charAt(index));
                        str = greedy.map(match -> match.str).orElse(null);
                    }
                }

                //STACK HANDLING
                Optional<PatternMatch> match = matches(current);
                if (match.isPresent()) {
                    handleMatch(match.get(), player, match.get().start && !isEnd(match.get(), tokens), tokens);
                    current = "";
                }

                //END THE LOOP
                if (index < string.length())
                    current += string.charAt(index++);
                else
                    break;
            }

            //Final string
            LiteralText result = new LiteralText("");
            addIfNonEmpty(current, tokens);
            Stack<Token> reverse = new Stack<>();
            while (!tokens.isEmpty())
                reverse.add(tokens.pop());
            while (!reverse.isEmpty()) {
                Token token = reverse.pop();
                if (token instanceof Token.TextToken)
                    result.append(((Token.TextToken) token).text());
                else {
                    addIfNonEmpty(((Token.MatchToken) token).match().exactMatch(), reverse);
                }
            }
            return result;
        } catch (Exception ignored) {
        }
        return new LiteralText("");
    }


    /*
    Logic controlling what to do when a match is found.
    1. If the match is escaped, remove the backslash and add the literal and return
    2. If the player does not have necessary permissions, add the literal and return
    3. Add the unmatched part of the string as a literal
    4. If the match is at the start, add it to the stack!
    5. If the match is at the end, find the first start match in the stack.
        a. If none exists, add the match as a literal
        b. If the start match exacts, apply formatting than push the elements back on the stack
           except the ends of the matches
     */
    private static void handleMatch(PatternMatch match, PlayerEntity player, boolean start, Stack<Token> tokens) {

        final String matched = match.str.substring(match.match.start, match.match.end);


        if ((match.match.start != 0 && match.str.charAt(match.match.start - 1) == '\\')) {
            addIfNonEmpty(match.str.substring(0, match.match.start - 1) + matched, tokens);
            return;
        }

        if (!match.data.condition.test(player)) {
            addIfNonEmpty(match.str, tokens);
            return;
        }

        addIfNonEmpty(match.str.substring(0, match.match.start), tokens);

        if (start) {
            if (match.data.generator == null)
                tokens.add(new Token.MatchToken(new Token.ExactMatch(match.data, match.start, matched)));
            else
                tokens.add(new Token.TextToken(match.data.generator.apply(matched, player)));
        } else {
            Stack<Token> transfer = new Stack<>();
            Supplier<Boolean> unmatched = () -> transfer.isEmpty() || !(transfer.peek() instanceof Token.MatchToken && match.data == ((Token.MatchToken) transfer.peek()).match().data());
            while (unmatched.get() && !tokens.isEmpty()) {
                transfer.add(tokens.pop());
            }
            if (!unmatched.get()) {
                String m2 = ((Token.MatchToken) transfer.pop()).match().exactMatch();
                transfer.forEach(i -> {
                    if (i instanceof Token.TextToken) {
                        MutableText text = ((Token.TextToken) i).text();
                        text.setStyle(match.data.modify.apply(text.getStyle(), m2));
                    }
                });
                while (!transfer.isEmpty())
                    tokens.add(transfer.pop());
            } else {
                while (!transfer.isEmpty())
                    tokens.add(transfer.pop());
                addIfNonEmpty(matched, tokens);
            }
        }
    }


    /*
    Rigorous method for determining whether a PatternData with the same start an end patterns,
    i.e. "**", is actually the end of such a match or the start of one.
     */
    public static boolean isEnd(PatternMatch match, Stack<Token> tokenStack) {
        return tokenStack.stream().anyMatch(i -> i instanceof Token.MatchToken && match.data == ((Token.MatchToken) i).match().data());
    }


    /*
    Master method for finding a match of a pattern in a given string
     */
    public static Optional<PatternMatch> matches(String str) {
        Optional<PatternData> startMatch = PATTERNS.stream().filter(i -> i.start.matcher(str).find()).findFirst();
        if (startMatch.isPresent())
            return Optional.of(new PatternMatch(startMatch.get(), true, str, matchIndex(startMatch.get().start, str).get()));
        else {
            Optional<PatternData> endMatch = PATTERNS.stream().filter(i -> i.stop.matcher(str).find()).findFirst();
            return endMatch.map(patternData -> new PatternMatch(patternData, false, str, matchIndex(patternData.stop, str).get()));
        }
    }


    /*
    Calculates the match index of the actual match of a pattern in a string
     */
    private static Optional<MatchIndex> matchIndex(Pattern pattern, String str) {
        Optional<String> match = match(pattern, str);
        if (match.isPresent()) {
            int start = str.indexOf(match.get());
            return Optional.of(new MatchIndex(start, start + match.get().length()));
        } else
            return Optional.empty();
    }

    /*
    Finds the matched substring of a string that matches a pattern
     */
    private static Optional<String> match(Pattern pattern, String str) {
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return Optional.of(matcher.group());
        } else {
            return Optional.empty();
        }
    }

    /*
    Used for [entity] and [me]. The hover event is broken
     */
    private static MutableText addEntityHover(Entity entity, MutableText text) {
        HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new HoverEvent.EntityContent(entity.getType(), entity.getUuid(), entity.getName()));
        text.setStyle(text.getStyle().withHoverEvent(event));
        return text;
    }

    record MatchIndex(int start, int end) {
    }

    /*
    Represents an entry in the patterns list.
    Takes in two patterns, the start and stop patterns to identify.
    Modify is a function representing how to modify the style of text within the identified pattern
    Generator represents a text generator: When not null, it will replace the contents of the pattern with the generated text.
    Condition represents the required condition for this pattern to not be ignored
     */
    record PatternData(Pattern start, Pattern stop, BiFunction<Style, String, Style> modify,
                       @Nullable BiFunction<String, PlayerEntity, MutableText> generator,
                       Predicate<PlayerEntity> condition) {


        PatternData(Pattern both, BiFunction<Style, String, Style> modify, @Nullable BiFunction<String, PlayerEntity, MutableText> generator, Predicate<PlayerEntity> condition) {
            this(both, both, modify, generator, condition);
        }

        @Override
        public String toString() {
            return "PatternData[" +
                    "start=" + start +
                    ", stop=" + stop +
                    ']';
        }
    }


    /*
     A pattern match represents a matched pattern in the string
     Contains a pattern data identifying the pattern that was matched, a boolean representing it was
     the start or the end which was matched, the string that was matched, and the indices of the actual match in the string
     */
    record PatternMatch(PatternData data, boolean start, String str, MatchIndex match) {
    }
}
