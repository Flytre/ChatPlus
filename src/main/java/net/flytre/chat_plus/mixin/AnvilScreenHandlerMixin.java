package net.flytre.chat_plus.mixin;

import net.flytre.chat_plus.ChatPlus;
import net.flytre.chat_plus.ClassicChatBuilder;
import net.flytre.chat_plus.config.Config;
import net.flytre.chat_plus.stack.StackChatBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {

    @Shadow
    private String newItemName;

    public AnvilScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    @Unique
    private static Text fromString(String string, PlayerEntity player) {
        return ChatPlus.CONFIG.getConfig().parser == Config.Parser.BUKKIT ? ClassicChatBuilder.fromString(string, player) : StackChatBuilder.fromString(string, player);
    }

    @Inject(method = "setNewItemName", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;setCustomName(Lnet/minecraft/text/Text;)Lnet/minecraft/item/ItemStack;", shift = At.Shift.AFTER))
    public void chat_plus$setItemName(String name, CallbackInfo ci) {
        ItemStack itemStack = this.getSlot(2).getStack();
        itemStack.setCustomName(fromString(this.newItemName, this.player));
    }

    @Redirect(method = "updateResult", at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"))
    public boolean chat_plus$stringEqualOverride(String newItemName, Object ignored) {
        ItemStack itemStack = this.input.getStack(0);
        return fromString(newItemName, this.player).equals(itemStack.getName());
    }

    @Redirect(method = "updateResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;setCustomName(Lnet/minecraft/text/Text;)Lnet/minecraft/item/ItemStack;"))
    public ItemStack chat_plus$setChatPlusName(ItemStack stack, Text name) {
        return stack.setCustomName(fromString(this.newItemName, this.player));
    }
}
