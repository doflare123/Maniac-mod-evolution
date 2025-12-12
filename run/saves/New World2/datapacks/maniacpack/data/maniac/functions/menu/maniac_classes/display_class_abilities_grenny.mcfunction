kill @e[tag=class_display]
kill @e[tag=class_interaction]

# Заголовок
execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~ ~ {Tags:["class_display","abilities_page"],text:'{"text":"Способности","color":"gold","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}

# Способность 1
execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~-0.5 ~ {Tags:["class_display","abilities_page"],text:'{"text":"• Бывший бэттер","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~-1.2 ~ {Tags:["class_display","abilities_page"],text:'{"text":" При ударе накладывает на жертву замедление\\n","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Способность 2
execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~-1.5 ~ {Tags:["class_display","abilities_page"],text:'{"text":"• Сильные руки","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~-2.5 ~ {Tags:["class_display","abilities_page"],text:'{"text":"  Если убивает одного из выживших, то она впадает в ярость и получает баф сила 1 на 20 секунд\\n","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Кнопка "< Назад"
execute as @e[tag=class_marker] at @s run summon minecraft:text_display ~ ~-3 ~-2 {Tags:["class_display","abilities_page"],text:'{"text":"< Назад","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker] at @s run summon minecraft:interaction ~ ~-3 ~-2 {Tags:["class_interaction","to_items_back"],width:1f,height:0.3f}

# Фон
execute as @e[tag=class_marker] at @s run summon text_display ~ ~-4 ~-0.5 {shadow:1b,Tags:["class_display","abilities_page"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[40f,17f,0f]},text:'{"text":" "}',background:1679234582,Rotation:[90f,0f]}