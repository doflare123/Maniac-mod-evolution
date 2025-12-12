kill @e[tag=class_display]
kill @e[tag=class_interaction]

# Заголовок
execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~ ~ {Tags:["class_display","items_page"],text:'{"text":"Предметы","color":"gold","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}

# Предмет 1
execute as @e[tag=class_marker] at @s run summon minecraft:item_display ~ ~-0.5 ~-1.5 {Tags:["class_display","items_page"],item:{id:"maniacweapons:bita",Count:1b},transformation:{scale:[0.5f,0.5f,0.1f]},Rotation:[90f,0f]}
execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~-0.7 ~0.2 {Tags:["class_display","items_page"],text:'{"text":"Деревянный меч\\nДля защиты от монстров","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Предмет 2
#execute as @e[tag=class_marker] at @s run summon minecraft:item_display ~ ~-1 ~-2 {Tags:["class_display","items_page"],item:{id:"minecraft:bread",Count:1b},transformation:{scale:[0.5f,0.5f,0.1f]},Rotation:[90f,0f]}
#execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~-1 ~ {Tags:["class_display","items_page"],text:'{"text":"Бита\\nпроссто бита)","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Предмет 3
execute as @e[tag=class_marker] at @s run summon minecraft:item_display ~ ~-2 ~-1.5 {Tags:["class_display","items_page"],item:{id:"minecraft:chainmail_chestplate",Count:1b},transformation:{scale:[-0.5f,-0.5f,-0.5f]},Rotation:[90f,0f]}
execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~-2.3 ~0.2 {Tags:["class_display","items_page"],text:'{"text":"Кольчужная броня\\nштаны и нагрудник","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Кнопка "< Назад"
execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~-3 ~-1.7 {Tags:["class_display","items_page"],text:'{"text":"< Назад","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker] at @s run summon minecraft:interaction ~ ~-3 ~-1.7 {Tags:["class_interaction","to_main"],width:1f,height:0.3f}

# Кнопка "Способности >"
execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~-3 ~1.4 {Tags:["class_display","items_page"],text:'{"text":"Навыки >","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker] at @s run summon minecraft:interaction ~ ~-3 ~1.4 {Tags:["class_interaction","to_abilities"],width:1.5f,height:0.3f}

# Фон
execute as @e[tag=class_marker] at @s run summon text_display ~ ~-4 ~-0.5 {shadow:1b,Tags:["class_display","abilities_page"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[35f,17f,-2f]},text:'{"text":" "}',background:1679234582,Rotation:[90f,0f]}