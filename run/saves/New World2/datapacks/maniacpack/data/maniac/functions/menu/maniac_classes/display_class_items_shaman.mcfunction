kill @e[tag=class_display_shaman]
kill @e[tag=class_interaction_shaman]

# Заголовок
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:text_display ~ ~ ~ {Tags:["class_display_shaman","items_page_shaman"],text:'{"text":"Предметы","color":"gold","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}

# Предмет 1
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:item_display ~0.5 ~-1.2 ~-2.0 {Tags:["class_display_shaman","items_page_shaman"],item:{id:"maniacweapons:totem_spawn_egg",Count:1b},transformation:{scale:[0.5f,0.5f,0.1f]},Rotation:[90f,0f]}
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:text_display ~ ~-1.2 ~0.4 {Tags:["class_display_shaman","items_page_shaman"],text:'{"text":"Призыв тотема\\nПоставьте тотем,\\nкоторый даёт\\nсопротивление вокруг","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Предмет 2
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:item_display ~ ~-2.3 ~-1.3 {Tags:["class_display_shaman","items_page_shaman"],item:{id:"maniacweapons:soul",Count:1b},transformation:{scale:[0.5f,0.5f,0.1f]},Rotation:[270f,0f]}
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:text_display ~ ~-2.1 ~0.2 {Tags:["class_display_shaman","items_page_shaman"],text:'{"text":"Душа предка\\nИсцеляет 8 хп","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Кнопка "< Назад"
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:text_display ~ ~-3 ~-1.7 {Tags:["class_display_shaman","items_page_shaman"],text:'{"text":"< Назад","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:interaction ~ ~-3 ~-1.7 {Tags:["class_interaction_shaman","to_main_shaman"],width:1f,height:0.3f}

# Кнопка "Способности >"
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:text_display ~ ~-3 ~1.4 {Tags:["class_display_shaman","items_page_shaman"],text:'{"text":"Навыки >","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:interaction ~ ~-3 ~1.4 {Tags:["class_interaction_shaman","to_abilities_shaman"],width:1.5f,height:0.3f}

# Фон
execute as @e[tag=class_marker_shaman] at @s run summon text_display ~ ~-4 ~-0.5 {shadow:1b,Tags:["class_display_shaman","items_page_shaman"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[35f,17f,-2f]},text:'{"text":" "}',background:1679234582,Rotation:[90f,0f]}