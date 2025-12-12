kill @e[tag=class_display_freddy]
kill @e[tag=class_interaction_freddy]

# Заголовок
execute as @e[tag=class_marker_freddy] at @s run summon minecraft:text_display ~ ~ ~ {Tags:["class_display_freddy","items_page_freddy"],text:'{"text":"Предметы","color":"gold","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}

# Предмет 1
execute as @e[tag=class_marker_freddy] at @s run summon minecraft:item_display ~ ~-0.5 ~-1.5 {Tags:["class_display_freddy","items_page_freddy"],item:{id:"maniacweapons:microphone",Count:1b},transformation:{scale:[0.5f,0.5f,0.1f]},Rotation:[270f,0f]}
execute as @e[tag=class_marker_freddy] at @s run summon minecraft:text_display ~ ~-0.9 ~0.2 {Tags:["class_display_freddy","items_page_freddy"],text:'{"text":"Микрофон\\nСтанит в радиусе 7 б.\\n4 урона","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Предмет 2
execute as @e[tag=class_marker_freddy] at @s run summon minecraft:item_display ~ ~-1.7 ~-1.5 {Tags:["class_display_freddy","items_page_freddy"],item:{id:"minecraft:leather_chestplate",Count:1b},transformation:{scale:[0.5f,0.5f,0.1f]},Rotation:[270f,0f]}
execute as @e[tag=class_marker_freddy] at @s run summon minecraft:text_display ~ ~-2.1 ~0.3 {Tags:["class_display_freddy","items_page_freddy"],text:'{"text":"Кожанная броня\\n\\n6 брони","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Кнопка "< Назад"
execute as @e[tag=class_marker_freddy] at @s run summon minecraft:text_display ~ ~-3 ~-1.7 {Tags:["class_display_freddy","items_page_freddy"],text:'{"text":"< Назад","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_freddy] at @s run summon minecraft:interaction ~ ~-3 ~-1.7 {Tags:["class_interaction_freddy","to_main_freddy"],width:1f,height:0.3f}

# Кнопка "Способности >"
execute as @e[tag=class_marker_freddy] at @s run summon minecraft:text_display ~ ~-3 ~1.4 {Tags:["class_display_freddy","items_page_freddy"],text:'{"text":"Навыки >","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_freddy] at @s run summon minecraft:interaction ~ ~-3 ~1.4 {Tags:["class_interaction_freddy","to_abilities_freddy"],width:1.5f,height:0.3f}

# Фон
execute as @e[tag=class_marker_freddy] at @s run summon text_display ~ ~-4 ~-0.5 {shadow:1b,Tags:["class_display_freddy","items_page_freddy"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[35f,17f,-2f]},text:'{"text":" "}',background:1679234582,Rotation:[90f,0f]}