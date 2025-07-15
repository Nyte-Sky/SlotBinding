package me.nytesky.slotbinding;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.util.Objects;

public class GuiKeyHandler {

    public static int state = 0; // 0: wating, 1: inventory, 2: hotbar
    public static int inv_slot;
    public static int hotbar_slot;

    @SubscribeEvent
    public void onGuiKeyPress(GuiScreenEvent.KeyboardInputEvent.Pre event) {

        // only care if the GUI is a container
        if (!(event.gui instanceof GuiContainer)) return;

        // only care about the key‑down action
        if (!Keyboard.getEventKeyState()) return;

        int    bind_key_code = KeyHandler.bindKey.getKeyCode();
        String bind_key_string = Keyboard.getKeyName(bind_key_code);

        int    keyCode = Keyboard.getEventKey();
        String keyName = Keyboard.getKeyName(keyCode);

        if (Objects.equals(keyName, bind_key_string)) {
            if (state == 0) {
                state = 1;
                SlotBinding.sendChat("fSelect inventory slot.");

            }
            else {
                state = 0;
                SlotBinding.sendChat("cSlot bind canceled.");

            }
            event.setCanceled(true);                // stop the inventory from handling it
        }
        else if (Objects.equals(keyName, "ESCAPE")) {
            if (state == 0) return;
            state = 0;
            SlotBinding.sendChat("cSlot bind canceled.");
        }
    }
}
