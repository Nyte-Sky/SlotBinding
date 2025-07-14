package me.nytesky.slotbinding;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = SlotBinding.MODID, version = SlotBinding.VERSION, name = SlotBinding.NAME)
public class SlotBinding {
    public static final String MODID = "slotbinding";
    public static final String NAME = "Slot Binding";
    public static final String VERSION = "1.0";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ConfigManager.load();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        System.out.println(NAME + " initialised.");

        KeyHandler.init();
        MinecraftForge.EVENT_BUS.register(new GuiKeyHandler());
        MinecraftForge.EVENT_BUS.register(new GuiClickHandler());
        MinecraftForge.EVENT_BUS.register(new HoverEffect());

    }
}