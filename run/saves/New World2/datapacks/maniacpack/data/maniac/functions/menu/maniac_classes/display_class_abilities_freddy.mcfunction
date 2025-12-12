kill @e[tag=class_display_freddy]
kill @e[tag=class_interaction_freddy]

# Заголовок
execute as @e[tag=class_marker_freddy] at @s run summon minecraft:text_display ~ ~ ~ {Tags:["class_display_freddy","abilities_page_freddy"],text:'{"text":"Способности","color":"gold","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}

# Способность 1
execute as @e[tag=class_marker_freddy] at @s run summon minecraft:text_display ~ ~-0.5 ~ {Tags:["class_display_freddy","abilities_page_freddy"],text:'{"text":"• Электричество","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_freddy] at @s run summon minecraft:text_display ~ ~-1.3 ~ {Tags:["class_display_freddy","abilities_page_freddy"],text:'{"text":"  Может выключить генератор\\nэлектричества, что потушит\\nвезде свет","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Кнопка "< Назад"
execute as @e[tag=class_marker_freddy] at @s run summon minecraft:text_display ~ ~-3 ~-2 {Tags:["class_display_freddy","abilities_page_freddy"],text:'{"text":"< Назад","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_freddy] at @s run summon minecraft:interaction ~ ~-3 ~-2 {Tags:["class_interaction_freddy","to_items_back_freddy"],width:1f,height:0.3f}

# Фон
execute as @e[tag=class_marker_freddy] at @s run summon text_display ~ ~-4 ~-0.5 {shadow:1b,Tags:["class_display_freddy","abilities_page_freddy"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[40f,17f,0f]},text:'{"text":" "}',background:1679234582,Rotation:[90f,0f]}