kill @e[tag=class_display_plague_doctor]
kill @e[tag=class_interaction_plague_doctor]

# Название класса
execute as @e[tag=class_marker_plague_doctor] at @s run summon minecraft:text_display ~ ~-0.5 ~ {Tags:["class_display_plague_doctor","main_page_plague_doctor"],text:'{"text":"Чумной доктор","color":"gold","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b,transformation:{scale:[1.5f,1.5f,1f]}}

# Описание класса
execute as @e[tag=class_marker_plague_doctor] at @s run summon minecraft:text_display ~ ~-1.5 ~ {Tags:["class_display_plague_doctor","main_page_plague_doctor"],text:'{"text":"Мои снадобья дарят\\nлибо жизнь, либо\\nвечный сон","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Кнопка "Выбрать класс"
execute as @e[tag=class_marker_plague_doctor] at @s run summon minecraft:text_display ~ ~-2.3 ~ {Tags:["class_display_plague_doctor","main_page_plague_doctor"],text:'{"text":"[Выбрать класс]","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_plague_doctor] at @s run summon minecraft:interaction ~ ~-2.3 ~ {Tags:["class_interaction","select_class_plague_doctor"],width:1.5f,height:0.3f}

# Стрелка "Предметы >"
execute as @e[tag=class_marker_plague_doctor] at @s run summon minecraft:text_display ~ ~-3 ~1 {Tags:["class_display_plague_doctor","main_page_plague_doctor"],text:'{"text":"Предметы >","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_plague_doctor] at @s run summon minecraft:interaction ~ ~-3 ~1 {Tags:["class_interaction_plague_doctor","to_items_plague_doctor"],width:1.2f,height:0.3f}

# Фон
execute as @e[tag=class_marker_plague_doctor] at @s run summon text_display ~ ~-4 ~ {shadow:1b,Tags:["class_display_plague_doctor","main_page_plague_doctor"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.4f,0f,0f],scale:[30f,15f,-2f]},text:'{"text":" "}',background:1679234582,Rotation:[90f,0f]}