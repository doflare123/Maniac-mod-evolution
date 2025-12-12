# Вычисляем процент: (Progress2.hack * 100) / Game.hackGoal
scoreboard players operation temp_percent temp_calc = Progress2 hack
scoreboard players set temp_hundred temp_calc 100
scoreboard players operation temp_percent temp_calc *= temp_hundred temp_calc
scoreboard players operation temp_percent temp_calc /= Game hackGoal

# Создаем text_display один раз при инициализации
execute unless entity @e[type=text_display,tag=hack2Eff,distance=..5] positioned ~ ~4 ~ run summon text_display ~ ~ ~ {Tags:["hack2Eff"],billboard:"center",text:'[{"text":"0%","color":"red","bold":true}]',background:0}

# Обновляем текст без пересоздания entity
execute if score comp2 hackGoal matches 0 if score temp_percent temp_calc matches 0..25 run data modify entity @e[type=text_display,tag=hack2Eff,distance=..5,limit=1] text set value '[{"score":{"name":"temp_percent","objective":"temp_calc"},"color":"red","bold":true},{"text":"%","color":"white"}]'
execute if score comp2 hackGoal matches 0 if score temp_percent temp_calc matches 26..50 run data modify entity @e[type=text_display,tag=hack2Eff,distance=..5,limit=1] text set value '[{"score":{"name":"temp_percent","objective":"temp_calc"},"color":"yellow","bold":true},{"text":"%","color":"white"}]'
execute if score comp2 hackGoal matches 0 if score temp_percent temp_calc matches 51..75 run data modify entity @e[type=text_display,tag=hack2Eff,distance=..5,limit=1] text set value '[{"score":{"name":"temp_percent","objective":"temp_calc"},"color":"gold","bold":true},{"text":"%","color":"white"}]'
execute if score comp2 hackGoal matches 0 if score temp_percent temp_calc matches 76..99 run data modify entity @e[type=text_display,tag=hack2Eff,distance=..5,limit=1] text set value '[{"score":{"name":"temp_percent","objective":"temp_calc"},"color":"dark_green","bold":true},{"text":"%","color":"white"}]'
execute if score comp2 hackGoal matches 0 if score temp_percent temp_calc matches 100 run data modify entity @e[type=text_display,tag=hack2Eff,distance=..5,limit=1] text set value '[{"text":"100%","color":"aqua","bold":true},{"text":" ✓","color":"lime"}]'
execute if score comp2 hackGoal matches 1 run data modify entity @e[type=text_display,tag=hack2Eff,distance=..5,limit=1] text set value '[{"text":"COMPLETED","color":"green","bold":true}]'
