kill @e[tag=class_display_scientist]
kill @e[tag=class_interaction_scientist]

# Заголовок
execute as @e[tag=class_marker_scientist] at @s run summon minecraft:text_display ~ ~ ~ {Tags:["class_display_scientist","abilities_page_scientist"],text:'{"text":"Способности","color":"gold","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}

# Способность 1
execute as @e[tag=class_marker_scientist] at @s run summon minecraft:text_display ~ ~-0.5 ~ {Tags:["class_display_scientist","abilities_page_scientist"],text:'{"text":"• Быстрый взлом","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_scientist] at @s run summon minecraft:text_display ~ ~-1.3 ~ {Tags:["class_display_scientist","abilities_page_scientist"],text:'{"text":"  Ваши знания позволяют быстрее\\n взламывать компьютеры\\nна 20%","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Способность 2
execute as @e[tag=class_marker_scientist] at @s run summon minecraft:text_display ~ ~-1.6 ~ {Tags:["class_display_scientist","abilities_page_scientist"],text:'{"text":"• Бонус генератора","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_scientist] at @s run summon minecraft:text_display ~ ~-2.6 ~ {Tags:["class_display_scientist","abilities_page_scientist"],text:'{"text":"  После каждого заряженного\\nкомпьютера к каждому\\nнезаряженному добавляется\\nпо 500 очков","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Кнопка "< Назад"
execute as @e[tag=class_marker_scientist] at @s run summon minecraft:text_display ~ ~-3 ~-2 {Tags:["class_display_scientist","abilities_page_scientist"],text:'{"text":"< Назад","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_scientist] at @s run summon minecraft:interaction ~ ~-3 ~-2 {Tags:["class_interaction_scientist","to_items_back_scientist"],width:1f,height:0.3f}

# Фон
execute as @e[tag=class_marker_scientist] at @s run summon text_display ~ ~-4 ~-0.5 {shadow:1b,Tags:["class_display_scientist","abilities_page_scientist"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[40f,17f,0f]},text:'{"text":" "}',background:1679234582,Rotation:[90f,0f]}