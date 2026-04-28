package org.example.maniacrevolution.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.checkerframework.checker.units.qual.K;
import org.example.maniacrevolution.Maniacrev;
import org.lwjgl.glfw.GLFW;

public class ModKeybinds {
    public static final String CATEGORY = "key.categories.maniacrev";
    public static final String CATEGORY2 = "key.categories.maniacrev";

    // ФИКС: Инициализируем СРАЗУ при объявлении, а не в методе
    public static final KeyMapping OPEN_GUIDE = new KeyMapping(
            "key.maniacrev.open_guide",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    public static final KeyMapping ACTIVATE_PERK = new KeyMapping(
            "key.maniacrev.activate_perk",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    public static final KeyMapping SWITCH_PERK = new KeyMapping(
            "key.maniacrev.switch_perk",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY
    );

    public static final KeyMapping QTE_KEY_1 = new KeyMapping(
            "key.maniacrev.qte1",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            CATEGORY2
    );

    public static final KeyMapping QTE_KEY_2 = new KeyMapping(
            "key.maniacrev.qte2",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    public static final KeyMapping QTE_KEY_3 = new KeyMapping(
            "key.maniacrev.qte3",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY
    );

    public static final KeyMapping QTE_KEY_4 = new KeyMapping(
            "key.maniacrev.qte4",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            CATEGORY
    );

    public static final KeyMapping ACTIVATE_ARMOR_ABILITY = new KeyMapping(
            "key.maniacrev.activate_armor_ability",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V, // По умолчанию клавиша V
            CATEGORY
    );

    // Этот метод теперь пустой, но оставляем для совместимости
    public static void register() {
        Maniacrev.LOGGER.debug("Keybinds initialized");
    }

    // Регистрация в Forge event
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GUIDE);
        event.register(ACTIVATE_PERK);
        event.register(SWITCH_PERK);
        event.register(QTE_KEY_1);
        event.register(QTE_KEY_2);
        event.register(QTE_KEY_3);
        event.register(QTE_KEY_4);
        event.register(ACTIVATE_ARMOR_ABILITY);
        Maniacrev.LOGGER.info("Keybinds registered");
    }
}
