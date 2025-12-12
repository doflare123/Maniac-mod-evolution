kill @e[tag=start_game]
execute as @e[tag=start_marker] at @s run summon minecraft:text_display ~-0.1 ~ ~ {Tags:["start_game"],text:'{"text":"Старт игры","color":"red"}',Rotation:[90f,0f],shadow:1b,background:0b}
execute as @e[tag=start_marker] at @s run summon minecraft:text_display ~-0.1 ~-1.5 ~ {Tags:["start_game"],text:'{"text":"[Пкм]","color":"green"}',Rotation:[90f,0f],shadow:1b,background:0b}

kill @e[tag=start_game_show]
execute as @e[tag=start_marker] at @s run summon minecraft:interaction ~0.5 ~-1 ~ {Tags:["start_game_show"],width:1f,height:0.4f}

kill @e[tag=background]
# Картинка как основной фон
execute as @e[tag=start_marker] at @s run summon minecraft:item_display ~ ~-0.5 ~ {Tags:["background","main_bg"],item:{id:"minecraft:paper",Count:1b,tag:{CustomModelData:2}},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[4f,2.5f,0.2f]},Rotation:[90f,0f]}

# Полупрозрачная подложка для читаемости текста
execute as @e[tag=start_marker] at @s run summon text_display ~ ~-1.2 ~ {shadow:1b,Tags:["background","text_bg"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[6f,2f,0.1f]},text:'{"text":" "}',background:1342177280,Rotation:[90f,0f]}