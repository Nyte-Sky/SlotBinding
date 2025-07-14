package me.nytesky.slotbinding;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Method;

public class GuiClickHandler {

    /* -------------------------------------------------------------------------
       Reflection: one-time lookup of GuiContainer#getSlotAtPosition
       MCP name  : getSlotAtPosition
       SRG name  : func_146975_c
       ------------------------------------------------------------------------- */
    private static final Method GET_SLOT_AT_POS;

    static {
        Method m;
        try {
            m = GuiContainer.class.getDeclaredMethod("getSlotAtPosition", int.class, int.class);
        } catch (NoSuchMethodException ignored) {
            // Dev environment not using MCP names? Try SRG / obf name.
            try {
                m = GuiContainer.class.getDeclaredMethod("func_146975_c", int.class, int.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Cannot find GuiContainer#getSlotAtPosition", e);
            }
        }
        m.setAccessible(true);
        GET_SLOT_AT_POS = m;
    }

    /* --------------------------------------------------------------------- */

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.gui instanceof GuiContainer)) return;

        // only fire on mouse‑button‑down, not up or scroll
        if (!Mouse.getEventButtonState()) return;
        int button = Mouse.getEventButton();
        if (button != 0 && button != 1) return;

        // convert raw display pixels into scaled GUI coords
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);

        int mouseX = Mouse.getEventX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() -
                (Mouse.getEventY() * sr.getScaledHeight() / mc.displayHeight) - 1;

        // ask the container which slot (if any) is there
        GuiContainer gui = (GuiContainer) event.gui;
        Slot clicked;
        try {
            clicked = (Slot) GET_SLOT_AT_POS.invoke(gui, mouseX, mouseY);
        } catch (Exception e) {
            // should never happen after static init, but cancel safely
            e.printStackTrace();
            return;
        }
        if (clicked == null) return;

        // exclude non-inventory slots
        if (clicked.inventory != mc.thePlayer.inventory) return;

        int slot = clicked.getSlotIndex();

        if (slot < 9) slot += 36;

        if (GuiKeyHandler.state == 1) SelectInventorySlot(slot, event);// select inv
        else if (GuiKeyHandler.state == 2) SelectHotbarSlot(slot, event);// select hotbar
        else if (GuiKeyHandler.state == 0) HandleSwapClick(slot, event);
    }
    public static void SelectInventorySlot(int slot, Event event){
        if (slot > 35){ // not inv
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new ChatComponentText("§6[SlotBinding] §eCannot select slot on hotbar.")
            );
        }
        else { // inv, so select and advance state
            GuiKeyHandler.inv_slot = slot;
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new ChatComponentText("§6[SlotBinding] §fSelected inventory slot!")
            );
            GuiKeyHandler.state = 2;
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new ChatComponentText("§6[SlotBinding] §fSelect hotbar slot.")
            );
        }
        event.setCanceled(true);
    }
    public static void SelectHotbarSlot(int slot, Event event){
            if (slot < 36){ // not hotbar
                Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText("§6[SlotBinding] §eCannot select slot in inventory")
                );
            }
            else { // hotbar, so select
                GuiKeyHandler.hotbar_slot = slot;
                Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText("§6[SlotBinding] §fSelected hotbar slot!")
                );
                GuiKeyHandler.state = 0;
                // now check if binding already exists
                if (!(ConfigManager.config.slotBinds.containsKey(GuiKeyHandler.inv_slot) &&
                        ConfigManager.config.slotBinds.get(GuiKeyHandler.inv_slot) == GuiKeyHandler.hotbar_slot)) {
                    // doesn't exist
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText("§6[SlotBinding] §aAdded inventory-hotbar binding!")
                    );
                    // so add
                    ConfigManager.config.slotBinds.put(GuiKeyHandler.inv_slot, GuiKeyHandler.hotbar_slot);
                    ConfigManager.save();
                }
                else {
                    // exists
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText("§6[SlotBinding] §cRemoved inventory-hotbar binding.")
                    );
                    // so remove
                    ConfigManager.config.slotBinds.remove(GuiKeyHandler.inv_slot, GuiKeyHandler.hotbar_slot);
                    ConfigManager.save();
                }
            }
            event.setCanceled(true);
    }

    public static void HandleSwapClick(int inv_slot, Event event){
        if (inv_slot > 35) return; // not inventory
        // not holding shift -> return
        if (!(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) return;
        // not bound -> return
        if (!(ConfigManager.config.slotBinds.containsKey(inv_slot))) return;

        Minecraft mc = Minecraft.getMinecraft();

        int hotbar_slot = ConfigManager.config.slotBinds.get(inv_slot) - 36; // hotbar slot 0-8
        EntityPlayerSP player = mc.thePlayer;
        int window_id = player.openContainer.windowId;

        mc.playerController.windowClick(window_id, inv_slot, hotbar_slot, 2, player);
        event.setCanceled(true);
    }

    public static void fireGuiKey(int keyCode) {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen == null) return;               // not inside a GUI

        // 1. convert KEY_? into the character GuiContainer expects
        char ch = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ||
                Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
                ? Keyboard.getKeyName(keyCode).toUpperCase().charAt(0)
                : Keyboard.getKeyName(keyCode).toLowerCase().charAt(0);

        try {
            Method keyTyped = GuiScreen.class.getDeclaredMethod("keyTyped", char.class, int.class);
            keyTyped.setAccessible(true);
            keyTyped.invoke(screen, ch, keyCode); // the inventory now thinks the user pressed the key
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}