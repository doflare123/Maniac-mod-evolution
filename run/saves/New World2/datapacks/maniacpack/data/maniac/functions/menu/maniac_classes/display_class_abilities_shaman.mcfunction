kill @e[tag=class_display_shaman]
kill @e[tag=class_interaction_shaman]

# Заголовок
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:text_display ~ ~ ~ {Tags:["class_display_shaman","abilities_page_shaman"],text:'{"text":"Способности","color":"gold","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}

# Способность 1
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:text_display ~ ~-0.5 ~ {Tags:["class_display_shaman","abilities_page_shaman"],text:'{"text":"• Поглощение души","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:text_display ~ ~-1.3 ~ {Tags:["class_display_shaman","abilities_page_shaman"],text:'{"text":"  Поглотите душу предка,\\nчтобы получить немного\\nжизненной силы","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Способность 2
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:text_display ~ ~-1.7 ~ {Tags:["class_display_shaman","abilities_page_shaman"],text:'{"text":"• Духи предков","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:text_display ~ ~-2.4000000000000004 ~ {Tags:["class_display_shaman","abilities_page_shaman"],text:'{"text":"  Духи предков подсказывают,\\nесли рядом с вами маньяк","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Кнопка "< Назад"
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:text_display ~ ~-3 ~-2 {Tags:["class_display_shaman","abilities_page_shaman"],text:'{"text":"< Назад","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_shaman] at @s run summon minecraft:interaction ~ ~-3 ~-2 {Tags:["class_interaction_shaman","to_items_back_shaman"],width:1f,height:0.3f}

# Фон
execute as @e[tag=class_marker_shaman] at @s run summon text_display ~ ~-4 ~-0.5 {shadow:1b,Tags:["class_display_shaman","abilities_page_shaman"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[40f,17f,0f]},text:'{"text":" "}',background:1679234582,Rotation:[90f,0f]}