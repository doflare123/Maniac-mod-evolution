kill @e[tag=class_display]
kill @e[tag=class_interaction]

# Название класса
execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~-0.5 ~ {Tags:["class_display","main_page"],text:'{"text":"Бабка Гренни","color":"gold","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b,transformation:{scale:[1.5f,1.5f,1f]}}

# Картинка класса (CustomModelData:1 - картинка Гренни)
# execute as @e[tag=class_marker] at @s run summon minecraft:item_display ~ ~0 ~ {Tags:["class_display","main_page"],item:{id:"minecraft:paper",Count:1b,tag:{CustomModelData:1}},transformation:{scale:[3f,3f,0.1f]},Rotation:[90f,0f]}

# Описание класса
execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~-1.5 ~ {Tags:["class_display","main_page"],text:'{"text":"Старая бабуля с острым\\nслухом и крепкими руками","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Кнопка "Выбрать класс"
execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~-2.3 ~ {Tags:["class_display","main_page"],text:'{"text":"[Выбрать класс]","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker] at @s run summon minecraft:interaction ~ ~-2.3 ~ {Tags:["class_interaction","select_class"],width:1.5f,height:0.3f}

# Стрелка "Предметы >"
execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~-3 ~1 {Tags:["class_display","main_page"],text:'{"text":"Предметы >","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker] at @s run summon minecraft:interaction ~ ~-3 ~1 {Tags:["class_interaction","to_items"],width:1.2f,height:0.3f}

# Фон
execute as @e[tag=class_marker] at @s run summon text_display ~ ~-4 ~ {shadow:1b,Tags:["class_display","abilities_page"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.4f,0f,0f],scale:[30f,15f,-2f]},text:'{"text":" "}',background:1679234582,Rotation:[90f,0f]}