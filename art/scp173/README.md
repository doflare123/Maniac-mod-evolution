# SCP-173 — статичная модель

## Файлы

- `scp173.bbmodel` — редактируемый проект Blockbench, формат Minecraft Bedrock Entity, совместимая с GeckoLib кубическая геометрия. Текстура встроена в проект; отдельный PNG лежит рядом.
- `scp173.png` — итоговая RGBA-текстура 128×128.
- `preview.png` — фронтальный, трёхчетвертной и боковой виды фактической геометрии с итоговой текстурой. Превью получено отдельным программным рендером, не скриншотом Blockbench или Minecraft.
- `texture_source.png` — исходный лист, созданный встроенным imagegen по двум пользовательским референсам.
- `build_assets.py` — воспроизводимая сборка проекта и игровых ресурсов из исходного листа, Python + Pillow.
- `render_preview.py` — рендер для проверки, Python + Pillow + NumPy.

Игровые ресурсы:

```
src/main/resources/assets/maniacrev/geo/scp173.geo.json
src/main/resources/assets/maniacrev/textures/entity/scp173.png
src/main/resources/assets/maniacrev/animations/scp173.animation.json
```

## Выбор экспорта

В `build.gradle` уже используется GeckoLib Forge 1.20.1 версии 4.4.7. `KeeperNightmareModel` загружает `.geo.json`, PNG и `.animation.json`, а `KeeperNightmareRenderer` использует `GeoReplacedEntityRenderer` для модели игрока. Поэтому экспорт — геометрия 1.12.0, как у имеющегося `windigo.geo.json`, с покубовой UV-развёрткой. Java-модель и новый рендерер не добавлены. Существующий Warden использует отдельный ванильный путь, который эта модель не затрагивает.

Проект открывается как Bedrock Entity и не требует плагина для редактирования кубов. Для дальнейших анимаций его можно конвертировать в GeckoLib Animated Model через официальный плагин GeckoLib Animation Utils. После экспорта сохраняйте геометрию версии 1.12.0 для текущего GeckoLib 4.

## Масштаб и поза

- 22 куба; высота 36 единиц = 2,25 блока при масштабе рендерера 1.0. Геометрия и центры вращения равномерно увеличены на 12,5%, пропорции и UV сохранены.
- Обе подошвы находятся строго на Y=0. Корень `root` и центр поворота — `[0, 0, 0]`, между ступнями.
- Перед модели направлен по -Z; Y вверх. При экспорте знак X и вращения преобразованы по правилам Bedrock-кодека Blockbench.
- `body` поворачивается от таза; `head` — от основания головы; руки — от плеч, предплечья — от локтей; ноги — от бёдер.
- Иерархия: `root` → `body` → `head`, `left_arm` → `left_forearm`, `right_arm` → `right_forearm`; `left_leg` и `right_leg` — дочерние кости корня.
- Сгиб рук записан в исходных вращениях кубов. Кости не имеют анимационных дорожек.
- Границы видимости: ширина 1.6875 блока, высота 2.8125, центр `[0, 1.125, 0]`; это запас для отсечения рендера, не хитбокс.

Анимационный JSON содержит пустой объект `animations`, без idle, дыхания, шага, бега, прыжка, атаки и поворота головы. Это допустимый пустой ресурс для будущего `GeoModel#getAnimationResource`.

При последующей интеграции перемещайте и поворачивайте всю модель по позиции и yaw игрока. Не добавляйте limb swing, независимый head yaw/pitch, дыхание, swing руки или позу приседания. Геометрия сама по себе не отключает эти эффекты в будущем рендерере. Камера и хитбокс на этом этапе не изменены. Игровой код пока не подключает ресурсы.

## Проверка

Проверены JSON, иерархия костей, UV-границы, встроенная текстура и её совпадение с игровым PNG, соответствие кубов экспорту, высота и подошвы. Проверены три вида программного превью. Открытие в Blockbench не подтверждено: файловый диалог веб-версии завершился технической ошибкой, настольное приложение не найдено. Запуск в Minecraft не выполнялся, поскольку интеграция не входит в этот этап.

## Промпт текстуры

Использован встроенный imagegen. Два приложенных изображения служили только визуальными референсами.

> Square texture sheet split exactly vertically into two equal panels. Left: flat painted SCP-173 face on pale warm ivory cracked concrete, tall irregular dried brick-red central forehead stripe, two muted olive green round spots, two black oval spots beneath, narrow black central opening with ivory outline and small dark mouth surrounded by red. Right: only pale ivory cracked concrete, hairline cracks and pixel chips. Minecraft pixel art, effective 128×128 grid, uniform ambient colour, no perspective, sculpture, shadows, text or watermark.
