kill @e[tag=class_display_plague_doctor]
kill @e[tag=class_interaction_plague_doctor]

# Заголовок
execute as @e[tag=class_marker_plague_doctor] at @s run summon minecraft:text_display ~ ~ ~ {Tags:["class_display_plague_doctor","abilities_page_plague_doctor"],text:'{"text":"Способности","color":"gold","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}

# Способность 1
execute as @e[tag=class_marker_plague_doctor] at @s run summon minecraft:text_display ~ ~-0.5 ~ {Tags:["class_display_plague_doctor","abilities_page_plague_doctor"],text:'{"text":"• Чума","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_plague_doctor] at @s run summon minecraft:text_display ~ ~-1.3 ~ {Tags:["class_display_plague_doctor","abilities_page_plague_doctor"],text:'{"text":"  Вокруг вас в радиусе 5 блоков\\nна всех накладывается чума","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Способность 2
execute as @e[tag=class_marker_plague_doctor] at @s run summon minecraft:text_display ~ ~-1.7 ~ {Tags:["class_display_plague_doctor","abilities_page_plague_doctor"],text:'{"text":"• Чумной труп","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_plague_doctor] at @s run summon minecraft:text_display ~ ~-2.4 ~ {Tags:["class_display_plague_doctor","abilities_page_plague_doctor"],text:'{"text":"  При зарядке каждого генератора\\nвы поднимаете чумной труп","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Кнопка "< Назад"
execute as @e[tag=class_marker_plague_doctor] at @s run summon minecraft:text_display ~ ~-3 ~-2 {Tags:["class_display_plague_doctor","abilities_page_plague_doctor"],text:'{"text":"< Назад","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_plague_doctor] at @s run summon minecraft:interaction ~ ~-3 ~-2 {Tags:["class_interaction_plague_doctor","to_items_back_plague_doctor"],width:1f,height:0.3f}

# Фон
execute as @e[tag=class_marker_plague_doctor] at @s run summon text_display ~ ~-4 ~-0.5 {shadow:1b,Tags:["class_display_plague_doctor","abilities_page_plague_doctor"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[40f,17f,0f]},text:'{"text":" "}',background:1679234582,Rotation:[90f,0f]}