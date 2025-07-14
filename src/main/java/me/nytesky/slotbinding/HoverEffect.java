package me.nytesky.slotbinding;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.Sys;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class HoverEffect {

    /* -------------------------------------------------------------------------
       Reflection: GuiContainer#getSlotUnderMouse  (MCP) / func_147006_a (SRG)
       ------------------------------------------------------------------------- */
    private static final Method GET_SLOT_UNDER_MOUSE;

    static {
        Method m;
        try {
            m = GuiContainer.class.getDeclaredMethod("getSlotUnderMouse");
        } catch (NoSuchMethodException ignored) {
            try {
                m = GuiContainer.class.getDeclaredMethod("func_147006_a"); // SRG / obf
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Cannot find GuiContainer#getSlotUnderMouse", e);
            }
        }
        m.setAccessible(true);
        GET_SLOT_UNDER_MOUSE = m;
    }

    /* --------------------------------------------------------------------- */

    Field GUI_LEFT = tryGetField("guiLeft", "field_147003_i");
    Field GUI_TOP  = tryGetField("guiTop",  "field_147009_r");

    private static Field tryGetField(String mcp, String srg) {
        try {
            Field f = GuiContainer.class.getDeclaredField(mcp);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e1) {
            try {
                Field f = GuiContainer.class.getDeclaredField(srg);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e2) {
                throw new RuntimeException("Cannot find field " + mcp + " or " + srg, e2);
            }
        }
    }

    /** Cached for the current DrawScreen event */
    private Slot hoveredSlot = null;

    @SubscribeEvent
    public void onGuiDraw(GuiScreenEvent.DrawScreenEvent.Post e) {
        if (!(e.gui instanceof GuiContainer)) return;

        GuiContainer gui = (GuiContainer) e.gui;
        hoveredSlot = null; // reset each frame

        /* 1. Cache hovered slot via reflection */
        try {
            hoveredSlot = (Slot) GET_SLOT_UNDER_MOUSE.invoke(gui);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        if (hoveredSlot == null) return;                    // nothing under mouse
        if (hoveredSlot.inventory != Minecraft.getMinecraft().thePlayer.inventory) return;

        int slotIndex = hoveredSlot.getSlotIndex();         // 0‑44
        if (!ConfigManager.config.slotBinds.containsKey(slotIndex)) return;

        int correspondingHotbarSlotIndex = ConfigManager.config.slotBinds.get(slotIndex);

        overlaySlot(slotIndex, gui, 0x80FFD700);      // Hovered slot: gold
        overlaySlot(correspondingHotbarSlotIndex, gui, 0x8000FFFF); // Bound slot: cyan

    }

    private void overlaySlot(int slotIndex, GuiContainer gui, int colorARGB) {

        int invIndex = (slotIndex >= 36 && slotIndex <= 44) ? slotIndex - 36 : slotIndex;

        Minecraft mc = Minecraft.getMinecraft();
        Slot slot = gui.inventorySlots.getSlotFromInventory(mc.thePlayer.inventory, invIndex);
        if (slot == null) System.out.println("Slot with index " + slotIndex + " is null");
        if (slot == null) return;

        int guiLeft, guiTop;
        try {
            guiLeft = (int) GUI_LEFT.get(gui);
            guiTop  = (int) GUI_TOP.get(gui);
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
            return;
        }

        int x = guiLeft + slot.xDisplayPosition;
        int y = guiTop  + slot.yDisplayPosition;
        drawRect(x, y, x + 16, y + 16, colorARGB);
    }


    /* ---------------------------------------------------------------------
       Helper: draw a coloured rectangle (MC 1.8 style)
       --------------------------------------------------------------------- */
    private void drawRect(int left, int top, int right, int bottom, int argb) {
        float a = (argb >> 24 & 255) / 255.0F;
        float r = (argb >> 16 & 255) / 255.0F;
        float g = (argb >> 8  & 255) / 255.0F;
        float b = (argb       & 255) / 255.0F;

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        GlStateManager.pushMatrix();                // Save matrix
        GlStateManager.translate(0, 0, 200);        // Push overlay forward (between GUI & tooltip)

        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);           // Don't write to depth buffer

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(left,  bottom, 0).color(r, g, b, a).endVertex();
        wr.pos(right, bottom, 0).color(r, g, b, a).endVertex();
        wr.pos(right, top,    0).color(r, g, b, a).endVertex();
        wr.pos(left,  top,    0).color(r, g, b, a).endVertex();
        tess.draw();

        GlStateManager.depthMask(true);            // Restore state
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();                // Restore original transform
    }
}
