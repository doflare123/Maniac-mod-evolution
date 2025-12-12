execute if score hpBoost game matches 6 run effect give @a[team=survivors] minecraft:health_boost infinite 2
execute if score hpBoost game matches 6 run effect give @a[team=survivors,scores={SurvivorClass=3}] minecraft:health_boost infinite 7

execute if score hpBoost game matches 4 run effect give @a[team=survivors] minecraft:health_boost infinite 1
execute if score hpBoost game matches 4 run effect give @a[team=survivors,scores={SurvivorClass=3}] minecraft:health_boost infinite 6

execute if score hpBoost game matches 2 run effect give @a[team=survivors] minecraft:health_boost infinite 0
execute if score hpBoost game matches 2 run effect give @a[team=survivors,scores={SurvivorClass=3}] minecraft:health_boost infinite 5

execute if score hpBoost game matches 0 run effect give @a[team=survivors,scores={SurvivorClass=3}] minecraft:health_boost infinite 4
