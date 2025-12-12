execute if entity @a[team=maniac,scores={ManiacClass=1}] run effect give @a[team=maniac,scores={ManiacClass=1}] minecraft:strength 20 1
execute if entity @a[team=maniac,scores={ManiacClass=2}] run effect give @a[team=maniac,scores={ManiacClass=2}] minecraft:speed 10 1
function maniac:classes/deathbaff

# Hitman
give @a[team=maniac,scores={ManiacClass=4}] cgm:basic_bullet 2
scoreboard players set @a[scores={hitmanClass=1}] hitmanKill 0