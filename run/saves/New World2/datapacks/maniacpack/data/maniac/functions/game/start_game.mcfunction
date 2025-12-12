scoreboard players set Game game 1
scoreboard players set playing game 0
scoreboard players set @a ManiacClass 0
scoreboard players set @a SurvivorClass 0
scoreboard players set @a hackOpened 0
maniacrev phase 0

scoreboard players operation Time game = Game timerMax
team join maniac @r[distance=..100,team=!maniac,scores={maniacPick=0}]
execute if score maniacCount game matches 2.. run team join maniac @r[distance=..100,team=!maniac,scores={maniacPick=0}]
execute if score maniacCount game matches 3.. run team join maniac @r[distance=..100,team=!maniac,scores={maniacPick=0}]
scoreboard players set @a rndClass 0
function maniac:game/time_set
setblock -329 43 -79 minecraft:redstone_block destroy
team join survivors @a[team=!maniac]

gamemode adventure @a
clear @a

# Компы
function maniac:hacks/compdefault

effect clear @a
stopsound @a
effect give @a minecraft:regeneration 2 255

scoreboard players set @a[team=maniac] maniacPick 1
execute unless entity @a[scores={maniacPick=0}] run scoreboard players set @a maniacPick 0

effect give @a minecraft:speed infinite 3

execute if score Game rndClass matches 1 run setblock -360 43 -90 minecraft:redstone_block destroy

tp @a[team=survivors] -379 63 27
tp @a[team=maniac] -340 44 48


execute if score Game map matches 4 run effect give @a[team=maniac,scores={ManiacClass=9}] minecraft:slowness infinite 0 false

maniacrev perks clear @a
maniacrev perks open @a