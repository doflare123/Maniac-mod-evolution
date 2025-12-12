execute as @a[team=maniac,scores={ManiacClass=10,deathbaff=0}] run effect give @s maniacweapons:death_pulse infinite 0 false
execute as @a[team=maniac,scores={ManiacClass=10,deathbaff=1}] run effect give @s maniacweapons:death_pulse infinite 1 false
execute as @a[team=maniac,scores={ManiacClass=10,deathbaff=2}] run effect give @s maniacweapons:death_pulse infinite 2 false
execute as @a[team=maniac,scores={ManiacClass=10,deathbaff=2}] run scoreboard players set @s deathbaff 3
execute as @a[team=maniac,scores={ManiacClass=10,deathbaff=1}] run scoreboard players set @s deathbaff 2
execute as @a[team=maniac,scores={ManiacClass=10,deathbaff=0}] run scoreboard players set @s deathbaff 1