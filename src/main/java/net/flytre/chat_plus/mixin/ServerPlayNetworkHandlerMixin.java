package net.flytre.chat_plus.mixin;


import net.flytre.chat_plus.ChatPlus;
import net.flytre.chat_plus.ClassicChatBuilder;
import net.flytre.chat_plus.config.Config;
import net.flytre.chat_plus.stack.StackChatBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.filter.TextStream;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ServerPlayNetworkHandler.class, priority = 1500)
public abstract class ServerPlayNetworkHandlerMixin {


    @Shadow
    public ServerPlayerEntity player;

    private static Text fromString(String string, PlayerEntity player) {
        return ChatPlus.CONFIG.getConfig().parser == Config.Parser.BUKKIT ? ClassicChatBuilder.fromString(string, player) : StackChatBuilder.fromString(string, player);
    }

    @ModifyVariable(method = "handleMessage", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/server/filter/TextStream$Message;getRaw()Ljava/lang/String;"))
    public String chat_plus$formatCommand(String string, TextStream.Message message) {
        if (string.length() > 0 && string.charAt(0) == '/')
            return fromString(string, player).getString();
        else
            return string;
    }

    @ModifyVariable(
            method = "handleMessage",
            at = @At(value = "STORE"), ordinal = 1
    )
    private Text chat_plus$formatChat2(Text text, TextStream.Message message) {
        return message.getFiltered().isEmpty() ? null : new TranslatableText("chat.type.text", this.player.getDisplayName(), fromString(message.getFiltered(), player));
    }

    @ModifyVariable(
            method = "handleMessage",
            at = @At(value = "STORE"), ordinal = 0
    )
    private Text chat_plus$formatChat1(Text text, TextStream.Message message) {
        return message.getFiltered().isEmpty() ? null : new TranslatableText("chat.type.text", this.player.getDisplayName(), fromString(message.getFiltered(), player));
    }

}
