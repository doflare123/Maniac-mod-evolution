execute as @a[team=survivors,scores={SurvivorClass=8}, gamemode=!spectator] at @s run tp @p[team=survivors,gamemode=spectator] @s
execute as @a[team=survivors,scores={SurvivorClass=8}, gamemode=!spectator] at @s run gamemode survival @p[team=survivors,gamemode=spectator]
effect give @a[team=survivors,gamemode=survival] minecraft:slowness infinite 1
effect give @a[team=survivors,gamemode=survival] minecraft:instant_damage 1 1
effect give @a[team=survivors,gamemode=survival] minecraft:resistance 2 255
gamemode adventure @a[team=survivors,gamemode=survival]