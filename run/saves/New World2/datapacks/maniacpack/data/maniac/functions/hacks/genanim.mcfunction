execute unless entity @e[type=armor_stand, tag=CirclePart, distance=..3] run summon armor_stand ~ ~ ~ {Invisible:1b,Small:1b,NoBasePlate:1b,NoGravity:1b,Tags:["TCirclePart","CirclePart"]}
execute as @e[type=armor_stand,tag=TCirclePart,distance=..3] at @s run tp @s ~ ~ ~-2.5 -90 0
execute as @e[type=armor_stand,tag=TCirclePart,distance=..3] run tag @s remove TCirclePart
execute as @e[type=armor_stand,tag=CirclePart,distance=..3] at @s run tp @s ^ ^ ^0.22 ~5 ~
execute as @e[type=armor_stand,tag=CirclePart,distance=..3] at @s run particle minecraft:happy_villager ~ ~ ~ 0.15 0.15 0.15 0.01 3