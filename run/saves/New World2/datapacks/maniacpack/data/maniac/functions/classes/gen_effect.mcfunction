give @a[scores={SurvivalClass=5},gamemode=!spectator] minecraft:paper{display: {Name:"{\"text\":\"Бинт\"}"}} 2

execute if entity @a[team=maniac,scores={ManiacClass=3}] run effect give @a[team=survivors] minecraft:glowing 8 0

execute if entity @a[team=survivors,scores={SurvivorClass=4},gamemode=!spectator] run effect give @a[team=maniac] minecraft:glowing 10 0

execute if score Итого Complete matches 3 if entity @a[team=maniac,scores={ManiacClass=7},gamemode=!spectator] run effect give @a[team=survivors,gamemode=!spectator] minecraft:nausea 60 255

tp @a[team=maniac,scores={ManiacClass=8},gamemode=!spectator] @r[team=survivors,gamemode=!spectator]

execute if score Итого Complete = Итого allGoal run give @a[team=survivors,scores={SurvivorClass=7}] cgm:stun_grenade 1

execute if score Итого Complete matches 3 run give @a[team=survivors,scores={SurvivorClass=7}] cgm:stun_grenade 1

execute if entity @a[team=survivors,scores={SurvivorClass=8},gamemode=!spectator] if score Итого hack matches 3 run function maniac:classes/necromancer

# Нарко
give @a[scores={SurvivorClass=9},team=survivors,gamemode=!spectator] maniacweapons:adrenalain 1

# Учёный
execute as @a[team=survivors,scores={SurvivorClass=10},gamemode=!spectator,limit=2] run function maniac:classes/scientist

# Алхимик
loot give @a[team=survivors,scores={SurvivorClass=6},gamemode=!spectator] loot maniac:alchemy

# Чумной Доктор
execute if score Game map matches 1 if entity @a[team=maniac,scores={ManiacClass=6},gamemode=!spectator] at @e[type=minecraft:marker,sort=random,tag=plagueMansion,limit=1] run function maniac:classes/spawn_zombie
execute if score Game map matches 4 if entity @a[team=maniac,scores={ManiacClass=6},gamemode=!spectator] at @e[type=minecraft:marker,sort=random,tag=plagueFnaf,limit=1] run function maniac:classes/spawn_zombie

maniacrev glowing_perks