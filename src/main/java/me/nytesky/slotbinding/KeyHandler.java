package me.nytesky.slotbinding;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public class KeyHandler {
    public static KeyBinding bindKey;

    public static void init() {
        bindKey = new KeyBinding("key.slotbinding.bindKey", Keyboard.KEY_L,
                "key.categories.slotbinding");
        ClientRegistry.registerKeyBinding(bindKey);

    }
}
