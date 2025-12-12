# Основная функция для отображения заряда

# Вычисляем количество зеленых палочек (каждая палочка = 50 единиц заряда)
scoreboard players operation Game green_bars = gen hack
scoreboard players operation Game green_bars /= perCharge game

# Вычисляем количество красных палочек
scoreboard players set Game red_bars 20
scoreboard players operation Game red_bars -= Game green_bars

# Отображаем результат в зависимости от количества зеленых палочек
execute if score Game green_bars matches 20 run title @a actionbar ["",{"text":"████████████████████","color":"green"}]
execute if score Game green_bars matches 19 run title @a actionbar ["",{"text":"███████████████████","color":"green"},{"text":"█","color":"red"}]
execute if score Game green_bars matches 18 run title @a actionbar ["",{"text":"██████████████████","color":"green"},{"text":"██","color":"red"}]
execute if score Game green_bars matches 17 run title @a actionbar ["",{"text":"█████████████████","color":"green"},{"text":"███","color":"red"}]
execute if score Game green_bars matches 16 run title @a actionbar ["",{"text":"████████████████","color":"green"},{"text":"████","color":"red"}]
execute if score Game green_bars matches 15 run title @a actionbar ["",{"text":"███████████████","color":"green"},{"text":"█████","color":"red"}]
execute if score Game green_bars matches 14 run title @a actionbar ["",{"text":"██████████████","color":"green"},{"text":"██████","color":"red"}]
execute if score Game green_bars matches 13 run title @a actionbar ["",{"text":"█████████████","color":"green"},{"text":"███████","color":"red"}]
execute if score Game green_bars matches 12 run title @a actionbar ["",{"text":"████████████","color":"green"},{"text":"████████","color":"red"}]
execute if score Game green_bars matches 11 run title @a actionbar ["",{"text":"███████████","color":"green"},{"text":"█████████","color":"red"}]
execute if score Game green_bars matches 10 run title @a actionbar ["",{"text":"██████████","color":"green"},{"text":"██████████","color":"red"}]
execute if score Game green_bars matches 9 run title @a actionbar ["",{"text":"█████████","color":"green"},{"text":"███████████","color":"red"}]
execute if score Game green_bars matches 8 run title @a actionbar ["",{"text":"████████","color":"green"},{"text":"████████████","color":"red"}]
execute if score Game green_bars matches 7 run title @a actionbar ["",{"text":"███████","color":"green"},{"text":"█████████████","color":"red"}]
execute if score Game green_bars matches 6 run title @a actionbar ["",{"text":"██████","color":"green"},{"text":"██████████████","color":"red"}]
execute if score Game green_bars matches 5 run title @a actionbar ["",{"text":"█████","color":"green"},{"text":"███████████████","color":"red"}]
execute if score Game green_bars matches 4 run title @a actionbar ["",{"text":"████","color":"green"},{"text":"████████████████","color":"red"}]
execute if score Game green_bars matches 3 run title @a actionbar ["",{"text":"███","color":"green"},{"text":"█████████████████","color":"red"}]
execute if score Game green_bars matches 2 run title @a actionbar ["",{"text":"██","color":"green"},{"text":"██████████████████","color":"red"}]
execute if score Game green_bars matches 1 run title @a actionbar ["",{"text":"█","color":"green"},{"text":"███████████████████","color":"red"}]
execute if score Game green_bars matches 0 run title @a actionbar ["",{"text":"████████████████████","color":"red"}]