execute positioned ~ ~2 ~ as @a[distance=..2.5,gamemode=adventure,team=survivors,limit=4] if score comp6 hackGoal matches 0 run scoreboard players add Progress6 hack 8
execute positioned ~ ~2 ~ as @a[distance=..2.5,gamemode=adventure,team=survivors,scores={SurvivorClass=10},limit=4] if score comp6 hackGoal matches 0 run scoreboard players add Progress6 hack 2

execute if score comp6 hackGoal matches 0 run summon marker ~ ~2 ~ {Tags:["charging_marker","gen_6"]}
execute if score comp6 hackGoal matches 0 run scoreboard players set @e[type=marker,tag=charging_marker,distance=..5,limit=1,sort=nearest,scores={markerLife=..0}] markerLife 3
execute positioned ~ ~2 ~ run execute as @a[distance=..2.5,scores={hackOpened=0},team=survivors,gamemode=adventure] if score comp6 hackGoal matches 0 run function maniac:hacks/hack_openqte6

execute positioned ~ ~2 ~ run function maniac:hacks/genanim
execute positioned ~ ~ ~ if entity @a[distance=..2.5,gamemode=adventure,team=survivors] run function maniac:hacks/hack_effect6