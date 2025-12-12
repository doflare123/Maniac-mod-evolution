# Replace Stone
execute if score rndGen game matches 1 run setblock -266 26 -1 minecraft:stone
execute if score rndGen game matches 2 run setblock -204 32 -10 minecraft:stone
execute if score rndGen game matches 3 run setblock -260 42 -42 minecraft:stone
execute if score rndGen game matches 4 run setblock -201 43 -43 minecraft:stone
execute if score rndGen game matches 1 run setblock -463 128 94 minecraft:stone
execute if score rndGen game matches 2 run setblock -547 128 30 minecraft:stone
execute if score rndGen game matches 3 run setblock -509 128 90 minecraft:stone
execute if score rndGen game matches 4 run setblock -522 128 28 minecraft:stone
execute if score rndGen game matches 1 run setblock -249 73 -195 minecraft:stone
execute if score rndGen game matches 2 run setblock -282 72 -166 minecraft:stone
execute if score rndGen game matches 3 run setblock -271 63 -198 minecraft:stone
execute if score rndGen game matches 4 run setblock -245 55 -135 minecraft:stone
execute if score rndGen game matches 1 run setblock -510 45 4 minecraft:stone
execute if score rndGen game matches 2 run setblock -514 46 -43 minecraft:stone
execute if score rndGen game matches 3 run setblock -467 58 -84 minecraft:stone
execute if score rndGen game matches 4 run setblock -503 46 -131 minecraft:stone
# Spawn Lever
execute if score rndGen game matches 1 if score Game map matches 1 run setblock -266 26 -2 minecraft:lever[facing=north, powered=true]
execute if score rndGen game matches 2 if score Game map matches 1 run setblock -204 32 -11 minecraft:lever[facing=north, powered=true]
execute if score rndGen game matches 3 if score Game map matches 1 run setblock -259 42 -42 minecraft:lever[facing=east, powered=true]
execute if score rndGen game matches 4 if score Game map matches 1 run setblock -201 43 -42 minecraft:lever[facing=south, powered=true]
execute if score rndGen game matches 1 if score Game map matches 2 run setblock -463 128 93 minecraft:lever[facing=north, powered=true]
execute if score rndGen game matches 2 if score Game map matches 2 run setblock -548 128 30 minecraft:lever[facing=west, powered=true]
execute if score rndGen game matches 3 if score Game map matches 2 run setblock -508 128 90 minecraft:lever[facing=east, powered=true]
execute if score rndGen game matches 4 if score Game map matches 2 run setblock -522 128 29 minecraft:lever[facing=south, powered=true]
execute if score rndGen game matches 1 if score Game map matches 3 run setblock -249 73 -194 minecraft:lever[facing=south, powered=true]
execute if score rndGen game matches 2 if score Game map matches 3 run setblock -282 72 -165 minecraft:lever[facing=south, powered=true]
execute if score rndGen game matches 3 if score Game map matches 3 run setblock -271 63 -197 minecraft:lever[facing=south, powered=true]
execute if score rndGen game matches 4 if score Game map matches 3 run setblock -245 55 -136 minecraft:lever[facing=north, powered=true]
execute if score rndGen game matches 1 if score Game map matches 4 run setblock -510 45 5 minecraft:lever[facing=south, powered=true]
execute if score rndGen game matches 2 if score Game map matches 4 run setblock -514 46 -42 minecraft:lever[facing=south, powered=true]
execute if score rndGen game matches 3 if score Game map matches 4 run setblock -468 58 -84 minecraft:lever[facing=west, powered=true]
execute if score rndGen game matches 4 if score Game map matches 4 run setblock -502 46 -131 minecraft:lever[facing=east, powered=true]
# Activate Lever
execute if score rndGen game matches 1 run setblock -266 26 -1 minecraft:target
execute if score rndGen game matches 2 run setblock -204 32 -10 minecraft:target
execute if score rndGen game matches 3 run setblock -260 42 -42 minecraft:target
execute if score rndGen game matches 4 run setblock -201 43 -43 minecraft:target
execute if score rndGen game matches 1 run setblock -463 128 94 minecraft:target
execute if score rndGen game matches 2 run setblock -547 128 30 minecraft:target
execute if score rndGen game matches 3 run setblock -509 128 90 minecraft:target
execute if score rndGen game matches 4 run setblock -522 128 28 minecraft:target
execute if score rndGen game matches 1 run setblock -249 73 -195 minecraft:target
execute if score rndGen game matches 2 run setblock -282 72 -166 minecraft:target
execute if score rndGen game matches 3 run setblock -271 63 -198 minecraft:target
execute if score rndGen game matches 4 run setblock -245 55 -135 minecraft:target
execute if score rndGen game matches 1 run setblock -510 45 4 minecraft:target
execute if score rndGen game matches 2 run setblock -514 46 -43 minecraft:target
execute if score rndGen game matches 3 run setblock -467 58 -84 minecraft:target
execute if score rndGen game matches 4 run setblock -503 46 -131 minecraft:target