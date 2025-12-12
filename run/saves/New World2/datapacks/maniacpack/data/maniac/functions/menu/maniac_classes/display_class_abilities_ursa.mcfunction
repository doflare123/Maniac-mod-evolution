kill @e[tag=class_display_ursa]
kill @e[tag=class_interaction_ursa]

# Заголовок
execute as @e[tag=class_marker_ursa] at @s run summon minecraft:text_display ~ ~ ~ {Tags:["class_display_ursa","abilities_page_ursa"],text:'{"text":"Способности","color":"gold","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}

# Способность 1
execute as @e[tag=class_marker_ursa] at @s run summon minecraft:text_display ~ ~-0.3 ~ {Tags:["class_display_ursa","abilities_page_ursa"],text:'{"text":"• Острые когти","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_ursa] at @s run summon minecraft:text_display ~ ~-1.3 ~ {Tags:["class_display_ursa","abilities_page_ursa"],text:'{"text":"  Ваши острые когти разрывают\\nплоть жертвы, тем самым\\nс каждым ударом наносите\\nвсе больше урона","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Способность 2
execute as @e[tag=class_marker_ursa] at @s run summon minecraft:text_display ~ ~-1.6 ~ {Tags:["class_display_ursa","abilities_page_ursa"],text:'{"text":"• Прыжок","color":"green","bold":true}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_ursa] at @s run summon minecraft:text_display ~ ~-2.7 ~ {Tags:["class_display_ursa","abilities_page_ursa"],text:'{"text":"  Вы имеете возможность\\nпрыгать на большие\\nрасстояния за счет\\nсвоей силы","color":"white"}',Rotation:[90f,0f],shadow:1b,background:0b}

# Кнопка "< Назад"
execute as @e[tag=class_marker_ursa] at @s run summon minecraft:text_display ~ ~-3 ~-2 {Tags:["class_display_ursa","abilities_page_ursa"],text:'{"text":"< Назад","color":"yellow"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=class_marker_ursa] at @s run summon minecraft:interaction ~ ~-3 ~-2 {Tags:["class_interaction_ursa","to_items_back_ursa"],width:1f,height:0.3f}

# Фон
execute as @e[tag=class_marker_ursa] at @s run summon text_display ~ ~-4 ~-0.5 {shadow:1b,Tags:["class_display_ursa","abilities_page_ursa"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[40f,17f,0f]},text:'{"text":" "}',background:1679234582,Rotation:[90f,0f]}