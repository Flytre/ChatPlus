package net.flytre.chat_plus.config;

import com.google.gson.annotations.SerializedName;
import net.flytre.flytre_lib.api.config.annotation.Description;

public class Config {


    @Description("Whether the users require operator for bold/italic/etc. (formatted) text")
    @SerializedName("require_op_for_formatting")
    public boolean formatOp = true;

    @Description("Whether the users require operator for colored text")
    @SerializedName("require_op_for_colors")
    public boolean colorOp = false;

    @Description("Whether the users require operator to use [item]")
    @SerializedName("require_op_for_item_hover")
    public boolean hoverOp = false;

    @SerializedName("parser")
    @Description("markup uses a Discord style parser, while bukkit uses formatting codes like &e")
    public Parser parser = Parser.MARKUP;


    public enum Parser {
        @SerializedName("markup") MARKUP,
        @SerializedName("bukkit") BUKKIT
    }
}
