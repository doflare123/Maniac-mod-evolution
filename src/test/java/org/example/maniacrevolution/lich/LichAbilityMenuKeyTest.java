package org.example.maniacrevolution.lich;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.example.maniacrevolution.event.ArmorAbilityKeyHandler;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.lwjgl.glfw.GLFW;

final class LichAbilityMenuKeyTest {
    private static final class Menu extends Screen {
        boolean closed;
        Menu() {super(Component.literal("Lich input test"));}
        @Override public void onClose() {closed=true;}
    }
    static void verify() throws Exception {
        var binding=ModKeybinds.ACTIVATE_ARMOR_ABILITY;var original=binding.getKey();
        var edge=ArmorAbilityKeyHandler.class.getDeclaredField("wasPressed");edge.setAccessible(true);
        var down=KeyMapping.class.getDeclaredField("isDown");down.setAccessible(true);
        try {
            for(int key:new int[]{GLFW.GLFW_KEY_V,GLFW.GLFW_KEY_J}) {
                binding.setKey(InputConstants.Type.KEYSYM.getOrCreate(key));binding.setDown(false);edge.setBoolean(null,false);
                var menu=new Menu();
                if(ArmorAbilityKeyHandler.closeLichMenu(menu,GLFW.GLFW_KEY_K,0)||menu.closed)
                    throw new AssertionError("Unrelated key closed the menu");
                if(!ArmorAbilityKeyHandler.closeLichMenu(menu,key,0)||!menu.closed)
                    throw new AssertionError("Ability key did not close the menu");
                if(!edge.getBoolean(null)||!down.getBoolean(binding))
                    throw new AssertionError("Closing press can be treated as a fresh activation");
                binding.setDown(false); // The real keyboard handler delivers release this way.
            }
        } finally {binding.setKey(original);binding.setDown(false);edge.setBoolean(null,false);}
        System.out.println("Lich menu ability key: default/rebound keys close, unrelated keys stay, activation edge consumed.");
    }
}
