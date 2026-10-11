package org.example.maniacrevolution.character.survivor;

import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.character.CharacterClass;
import org.example.maniacrevolution.character.CharacterType;

public final class TechiesClass extends CharacterClass {
    public static final int SCOREBOARD_ID = 14;
    public TechiesClass() {
        super("techies", "Течис", CharacterType.SURVIVOR,
                "Сапёр: три типа мин, ручной подрыв и перераспределение маны.", SCOREBOARD_ID, 0);
        addTag("Ловушки"); addTag("Поддержка");
        addFeature("Красная мина", "V: установка 1 с, 5 маны, КД 5 с. Маньяк запускает 3 HP, отбрасывание и Замедление II на 2 с.");
        addFeature("Зелёная мина", "Установка 5 с, 10 маны, КД 30 с. Пульт: взрыв через 0,2 с, 6 HP, сильное отбрасывание, Замедление IV на 3 с.");
        addFeature("Синяя мина", "Установка 3 с, 20 маны, КД 10 с. До 15 фактической маны с каждой цели, отбрасывание и Замедление II на 2 с; урон 0.");
        addFeature("Осторожно", "Радиус 2 блока; стены защищают. Эффекты поражают также союзников и владельца. Движение и урон прерывают установку без возврата.");
        addItem("Каска сапёра", "Декоративная каска, не даёт защиты.");
        addItem("Пульт", "ПКМ: одна собственная зелёная, ближайшая к направлению взгляда; работает через стены, также в downed.");
    }
    @Override public ResourceLocation getSelectionFrescoTexture() {
        return Maniacrev.loc("textures/gui/frescos/techies_selection.png");
    }
    @Override public String getDifficultyStars() { return "Сложность не утверждена"; }
}
