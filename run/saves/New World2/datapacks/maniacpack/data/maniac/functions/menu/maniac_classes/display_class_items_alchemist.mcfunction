kill @e[tag=class_display_alchemist]
kill @e[tag=class_interaction_alchemist]

# Заголовок
execute as @e[tag=class_marker_alchemist] at @s run summon minecraft:text_display ~ ~ ~ {Tags:["class_display_alchemist","items_page_alchemist"],text:'{"text":"Предметы","color":"gold","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}

# Предмет 2
execute as @e[tag=class_marker_alchemist] at @s run summon minecraft:item_display ~ ~-0.5 ~-1.7 {Tags:["class_display_alchemist","items_page_alchemist"],item:{id:"minecraft:splash_potion",Count:1b},transformation:{scale:[1.0f,0.1f,0.1f]},Rotation:[270f,0f]}
execute as @e[tag=class_marker_alchemist] at @s run summon minecraft:text_display ~ ~-0.9 ~0.2 {Tags:["class_display_alchemist","items_page_alchemist"],text:'{"text":"5 взрывных зелья воды\\nдля варки","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Предмет 3
execute as @e[tag=class_marker_alchemist] at @s run summon minecraft:item_display ~ ~-1.7 ~-1.7 {Tags:["class_display_alchemist","items_page_alchemist"],item:{id:"minecraft:blaze_powder",Count:1b},transformation:{scale:[0.0f,0.1f,0.1f]},Rotation:[270f,0f]}
execute as @e[tag=class_marker_alchemist] at @s run summon minecraft:text_display ~ ~-2.4 ~0.2 {Tags:["class_display_alchemist","items_page_alchemist"],text:'{"text":"3 случайных\\nингредиента\\nдля варки зелий\\n+ огненный порошок","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Кнопка "< Назад"
execute as @e[tag=class_marker_alchemist] at @s run summon minecraft:text_display ~ ~-3 ~-1.7 {Tags:["class_display_alchemist","items_page_alchemist"],text:'{"text":"< Назад","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_alchemist] at @s run summon minecraft:interaction ~ ~-3 ~-1.7 {Tags:["class_interaction_alchemist","to_main_alchemist"],width:1f,height:0.3f}

# Кнопка "Способности >"
execute as @e[tag=class_marker_alchemist] at @s run summon minecraft:text_display ~ ~-3 ~1.4 {Tags:["class_display_alchemist","items_page_alchemist"],text:'{"text":"Навыки >","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_alchemist] at @s run summon minecraft:interaction ~ ~-3 ~1.4 {Tags:["class_interaction_alchemist","to_abilities_alchemist"],width:1.5f,height:0.3f}

# Фон
execute as @e[tag=class_marker_alchemist] at @s run summon text_display ~ ~-4 ~-0.5 {shadow:1b,Tags:["class_display_alchemist","items_page_alchemist"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[35f,17f,-2f]},text:'{"text":" "}',background:1679234582,Rotation:[90f,0f]}