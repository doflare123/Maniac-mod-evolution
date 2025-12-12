kill @e[tag=class_display_death]
kill @e[tag=class_interaction_death]

# Название класса
execute as @e[tag=class_marker_death] at @s run summon minecraft:text_display ~ ~-0.5 ~ {Tags:["class_display_death","main_page_death"],text:'{"text":"Смерть","color":"gold","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b,transformation:{scale:[1.5f,1.5f,1f]}}

# Описание класса
execute as @e[tag=class_marker_death] at @s run summon minecraft:text_display ~ ~-1.5 ~ {Tags:["class_display_death","main_page_death"],text:'{"text":"Memento mori","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Кнопка "Выбрать класс"
execute as @e[tag=class_marker_death] at @s run summon minecraft:text_display ~ ~-2.3 ~ {Tags:["class_display_death","main_page_death"],text:'{"text":"[Выбрать класс]","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_death] at @s run summon minecraft:interaction ~ ~-2.3 ~ {Tags:["class_interaction","select_class_death"],width:1.5f,height:0.3f}

# Стрелка "Предметы >"
execute as @e[tag=class_marker_death] at @s run summon minecraft:text_display ~ ~-3 ~1 {Tags:["class_display_death","main_page_death"],text:'{"text":"Предметы >","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_death] at @s run summon minecraft:interaction ~ ~-3 ~1 {Tags:["class_interaction_death","to_items_death"],width:1.2f,height:0.3f}

# Фон
execute as @e[tag=class_marker_death] at @s run summon text_display ~ ~-4 ~ {shadow:1b,Tags:["class_display_death","abilities_page_death"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.4f,0f,0f],scale:[30f,15f,-2f]},text:'{"text":" "}',background:1679234582,Rotation:[90f,0f]}