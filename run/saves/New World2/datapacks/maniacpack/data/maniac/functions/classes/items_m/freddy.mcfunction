item replace entity @a[team=maniac,scores={ManiacClass=12}] armor.head with minecraft:player_head{display:{Name:'{"text":"Фредди","color":"gold","underlined":true,"bold":true,"italic":false}',Lore:['{"text":"ID головы: 898","color":"gray","italic":false}','{"text":"mcheads.ru","color":"blue","italic":false}']},SkullOwner:{Id:[I;2080793942,-524468218,-1541115779,1949756395],Properties:{textures:[{Value:"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTcxMWRjZWM1Yjk4OTdhMzI3ZjY4Yjk5MjI3MWNiYTg0NTExYzBhMDQzZTc4ZGY1ZWJmOTlhMDZmNGU4ZjVhIn19fQ=="}]}}} 1
item replace entity @a[team=maniac,scores={ManiacClass=12}] armor.chest with minecraft:leather_chestplate 1
item replace entity @a[team=maniac,scores={ManiacClass=12}] armor.legs with minecraft:leather_leggings 1
item replace entity @a[team=maniac,scores={ManiacClass=12}] armor.feet with minecraft:leather_boots 1
give @a[team=maniac,scores={ManiacClass=12}] maniacweapons:microphone
execute if entity @a[team=maniac,scores={ManiacClass=12}] run scoreboard players set gen hack 500
execute if entity @a[team=maniac,scores={ManiacClass=12}] run setblock -329 43 -62 minecraft:redstone_block destroy
effect give @a[team=maniac,scores={ManiacClass=12}] minecraft:slowness infinite 0

execute if entity @a[team=maniac,scores={ManiacClass=12}] run playsound maniacweapons:hello block @a