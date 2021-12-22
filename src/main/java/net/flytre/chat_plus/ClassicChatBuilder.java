package net.flytre.chat_plus;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

import java.util.regex.Pattern;

public class ClassicChatBuilder {


    private static final Pattern REPLACE_COLOR = Pattern.compile("[0-9a-g]|r", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPLACE_FORMATTING = Pattern.compile("[k-o]|r", Pattern.CASE_INSENSITIVE);


    public static Text fromString(String string, PlayerEntity playerEntity) {
        LiteralText result = new LiteralText("");


        StringBuilder current = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            String flexCheck = string.substring(i, Math.min(string.length(), i + 6));
            if (flexCheck.equals("[item]") && shouldFlex(playerEntity)) {
                result.append(new LiteralText(current.toString()));
                current = new StringBuilder();
                i += 5;
                Text text = playerEntity.getMainHandStack().toHoverableText();
                result.append(text);
            } else {
                char ch = string.charAt(i);
                if (ch == '&' && i < string.length() - 1) {
                    char nx = string.charAt(i + 1);
                    if (passesOpCheck(playerEntity, ChatPlus.CONFIG.getConfig().colorOp) && REPLACE_COLOR.matcher(nx + "").find()) {
                        ch = '§';
                    }
                    if (passesOpCheck(playerEntity, ChatPlus.CONFIG.getConfig().formatOp) && REPLACE_FORMATTING.matcher(nx + "").find()) {
                        ch = '§';
                    }
                }
                current.append(ch);
            }
        }
        result.append(new LiteralText(current.toString()));

        return result;
    }

    public static boolean shouldFlex(PlayerEntity playerEntity) {
        return passesOpCheck(playerEntity, ChatPlus.CONFIG.getConfig().hoverOp) && !playerEntity.getMainHandStack().isEmpty();
    }

    public static boolean passesOpCheck(PlayerEntity playerEntity, boolean requiresOP) {
        return !requiresOP || playerEntity.isCreativeLevelTwoOp();
    }
}
