kill @e[tag=class_display_death]
kill @e[tag=class_interaction_death]

# Заголовок
execute as @e[tag=class_marker_death] at @s run summon minecraft:text_display ~ ~ ~ {Tags:["class_display_death","abilities_page_death"],text:'{"text":"Способности","color":"gold","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}

# Способность 1
execute as @e[tag=class_marker_death] at @s run summon minecraft:text_display ~ ~-0.5 ~ {Tags:["class_display_death","abilities_page_death"],text:'{"text":"• Рабочие будни","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_death] at @s run summon minecraft:text_display ~ ~-1.3 ~ {Tags:["class_display_death","abilities_page_death"],text:'{"text":" Видит в радиусе 7 блоков\\nвыживших, которые имеют\\nменьше 10 хп","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Способность 2
execute as @e[tag=class_marker_death] at @s run summon minecraft:text_display ~ ~-1.6 ~ {Tags:["class_display_death","abilities_page_death"],text:'{"text":"• Судный час","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_death] at @s run summon minecraft:text_display ~ ~-2.5 ~ {Tags:["class_display_death","abilities_page_death"],text:'{"text":"  Раз в 1.5 минуты можно телепотироваться\\nк случайной жертве","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Кнопка "< Назад"
execute as @e[tag=class_marker_death] at @s run summon minecraft:text_display ~ ~-3 ~-2 {Tags:["class_display_death","abilities_page_death"],text:'{"text":"< Назад","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_death] at @s run summon minecraft:interaction ~ ~-3 ~-2 {Tags:["class_interaction_death","to_items_back_death"],width:1f,height:0.3f}

# Фон
execute as @e[tag=class_marker_death] at @s run summon text_display ~ ~-4 ~-0.5 {shadow:1b,Tags:["class_display_death","abilities_page_death"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[40f,17f,0f]},text:'{"text":" "}',background:1679234582,Rotation:[90f,0f]}