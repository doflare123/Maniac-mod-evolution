execute as @e[type=minecraft:interaction,tag=start_game_show] if data entity @s interaction run function maniac:game/start_game
execute as @e[type=minecraft:interaction,tag=start_game_show] if data entity @s interaction run data remove entity @s interaction

# ===========================================
# УДАЛЕНИЕ ОПЫТА
# ===========================================
kill @e[type=experience_orb]

# ===========================================
# ОБРАБОТКА СТАРТА ИГРЫ
# ===========================================
execute if score Game game matches 1 if score playing game matches 0 unless entity @a[scores={ManiacClass=0},team=maniac] unless entity @a[scores={SurvivorClass=0},team=survivors] run function maniac:game/play_game


# ===========================================
# ОБРАБОТКА ПОБЕДЫ
# ===========================================
execute if entity @a[team=survivors,gamemode=spectator] unless entity @a[team=survivors,gamemode=!spectator] if score Game game matches 1 run function maniac:game/game_end
execute if entity @a[team=maniac,gamemode=spectator] unless entity @a[team=maniac,gamemode=!spectator] if score Game game matches 1 run function maniac:game/game_end
execute if score @r PlayerLevel matches ..0 if score Game game matches 1 run function maniac:game/game_end


# ===========================================
# ОБРАБОТКА СМЕРТИ
# ===========================================
execute if entity @a[scores={death=1..}] run function maniac:game/death


# ===========================================
# ОБРАБОТКА ГЕНЕРАТОРА ФРЕДДИ
# ===========================================
execute if entity @a[team=maniac,scores={ManiacClass=12}] run function maniac:classes/freddy/charge_display



# ===========================================
# ОБРАБОТКА ВЫДАЧИ КАРТОЧКИ
# ===========================================
execute if score Итого Complete >= Game allGoal if score Итого Complete matches ..90 run function maniac:game/complete_comps


# ===========================================
# ОБРАБОТКА КЛАССОВЫХ ПРИКОЛОВ
# ===========================================
function maniac:classes/shaman_spirits
function maniac:classes/freddy/fnafgenremove
execute if entity @a[team=maniac,scores={ManiacClass=12}] if score gen hack matches ..1 run effect give @a[team=survivors, gamemode=adventure] minecraft:darkness infinite 0
execute if entity @a[team=maniac,scores={ManiacClass=12}] if score gen hack matches 999.. run effect clear @a[team=survivors, gamemode=adventure] minecraft:darkness
execute as @a[team=maniac,scores={ManiacClass=9},gamemode=!spectator] at @s run effect give @a[distance=..3,team=survivors,gamemode=!spectator] minecraft:glowing 5 0
# Чумной Доктор
execute as @a[nbt={SelectedItem:{id:"minecraft:wooden_sword",tag:{display:{Name:"{\"text\":\"Коса\"}"}}}}] at @s run effect give @a[distance=..5,sort=random,team=!maniac,nbt=!{ActiveEffects:[{"forge:id":"maniac_weapons:plague"}]}] maniacweapons:plague 4 0
execute as @e[type=zombie,tag=plague_zombie] at @s run effect give @a[team=survivors,gamemode=!spectator,distance=..3] maniacweapons:plague 7 0

# Смерть
execute as @a[team=maniac,scores={ManiacClass=10},gamemode=!spectator] at @s run effect give @a[team=survivors,gamemode=!spectator,distance=..7,nbt={Health:[0.0f,9.0f]}] minecraft:glowing 1 0 true

execute if score Итого Complete matches 100.. if score @r PlayerLevel matches ..90 run effect give @a[team=maniac,gamemode=!spectator] minecraft:glowing 90 0

# Подсказка алхимику
execute if entity @a[team=survivors,scores={SurvivorClass=6}] as @e[type=marker,tag=brewSpawn] at @s run particle minecraft:witch ~ ~1 ~ -0.5 0.5 0.5 1 1 force @a[team=survivors,scores={SurvivorClass=6}]

# Закрытие хака компа
# Проверяем игроков с hackOpened=1
# Если рядом НЕТ маркера - останавливаем хакинг
execute as @a[scores={hackOpened=1}] at @s unless entity @e[type=marker,tag=charging_marker,distance=..2.5] run function maniac:hacks/hack_closeqte

# Удаление маркеров зарядки
scoreboard players remove @e[type=marker,tag=charging_marker] markerLife 1
kill @e[type=marker,tag=charging_marker,scores={markerLife=..0}]

# ===========================================
# ОБРАБОТЧИКИ КЛИКОВ ДЛЯ ВСЕХ КЛАССОВ
# ===========================================

# ===========================================
# ОБРАБОТКА КЛИКОВ МИШКА ФРЕДДИ
# ===========================================

execute as @e[tag=select_class_freddy] on target run scoreboard players set @s ManiacClass 12
execute as @e[tag=select_class_freddy] on target run tellraw @s "Вы выбрали Мишка Фредди"
execute as @e[tag=select_class_freddy] run data remove entity @s interaction

execute as @e[tag=to_items_freddy] on target run function maniac:menu/maniac_classes/display_class_items_freddy
execute as @e[tag=to_items_freddy] run data remove entity @s interaction

execute as @e[tag=to_main_freddy] on target run function maniac:menu/maniac_classes/display_class_main_freddy
execute as @e[tag=to_main_freddy] run data remove entity @s interaction

execute as @e[tag=to_abilities_freddy] on target run function maniac:menu/maniac_classes/display_class_abilities_freddy
execute as @e[tag=to_abilities_freddy] run data remove entity @s interaction

execute as @e[tag=to_items_back_freddy] on target run function maniac:menu/maniac_classes/display_class_items_freddy
execute as @e[tag=to_items_back_freddy] run data remove entity @s interaction

# ===========================================
# ОБРАБОТКА КЛИКОВ ЧУМНОЙ ДОКТОР
# ===========================================

execute as @e[tag=select_class_plague_doctor] on target run scoreboard players set @s ManiacClass 6
execute as @e[tag=select_class_plague_doctor] on target run tellraw @s "Вы выбрали Чумной доктор"
execute as @e[tag=select_class_plague_doctor] run data remove entity @s interaction

execute as @e[tag=to_items_plague_doctor] on target run function maniac:menu/maniac_classes/display_class_items_plague_doctor
execute as @e[tag=to_items_plague_doctor] run data remove entity @s interaction

execute as @e[tag=to_main_plague_doctor] on target run function maniac:menu/maniac_classes/display_class_main_plague_doctor
execute as @e[tag=to_main_plague_doctor] run data remove entity @s interaction

execute as @e[tag=to_abilities_plague_doctor] on target run function maniac:menu/maniac_classes/display_class_abilities_plague_doctor
execute as @e[tag=to_abilities_plague_doctor] run data remove entity @s interaction

execute as @e[tag=to_items_back_plague_doctor] on target run function maniac:menu/maniac_classes/display_class_items_plague_doctor
execute as @e[tag=to_items_back_plague_doctor] run data remove entity @s interaction

# ===========================================
# ОБРАБОТКА КЛИКОВ УРСА
# ===========================================

execute as @e[tag=select_class_ursa] on target run scoreboard players set @s ManiacClass 7
execute as @e[tag=select_class_ursa] on target run tellraw @s "Вы выбрали Урса"
execute as @e[tag=select_class_ursa] run data remove entity @s interaction

execute as @e[tag=to_items_ursa] on target run function maniac:menu/maniac_classes/display_class_items_ursa
execute as @e[tag=to_items_ursa] run data remove entity @s interaction

execute as @e[tag=to_main_ursa] on target run function maniac:menu/maniac_classes/display_class_main_ursa
execute as @e[tag=to_main_ursa] run data remove entity @s interaction

execute as @e[tag=to_abilities_ursa] on target run function maniac:menu/maniac_classes/display_class_abilities_ursa
execute as @e[tag=to_abilities_ursa] run data remove entity @s interaction

execute as @e[tag=to_items_back_ursa] on target run function maniac:menu/maniac_classes/display_class_items_ursa
execute as @e[tag=to_items_back_ursa] run data remove entity @s interaction

# ===========================================
# ОБРАБОТКА КЛИКОВ АЛХИМИК
# ===========================================

execute as @e[tag=select_class_alchemist] on target run scoreboard players set @s SurvivorClass 6
execute as @e[tag=select_class_alchemist] on target run tellraw @s "Вы выбрали Алхимик"
execute as @e[tag=select_class_alchemist] run data remove entity @s interaction

execute as @e[tag=to_items_alchemist] on target run function maniac:menu/maniac_classes/display_class_items_alchemist
execute as @e[tag=to_items_alchemist] run data remove entity @s interaction

execute as @e[tag=to_main_alchemist] on target run function maniac:menu/maniac_classes/display_class_main_alchemist
execute as @e[tag=to_main_alchemist] run data remove entity @s interaction

execute as @e[tag=to_abilities_alchemist] on target run function maniac:menu/maniac_classes/display_class_abilities_alchemist
execute as @e[tag=to_abilities_alchemist] run data remove entity @s interaction

execute as @e[tag=to_items_back_alchemist] on target run function maniac:menu/maniac_classes/display_class_items_alchemist
execute as @e[tag=to_items_back_alchemist] run data remove entity @s interaction

# ===========================================
# ОБРАБОТКА КЛИКОВ ШАМАН
# ===========================================

execute as @e[tag=select_class_shaman] on target run scoreboard players set @s SurvivorClass 1
execute as @e[tag=select_class_shaman] on target run tellraw @s "Вы выбрали Шаман"
execute as @e[tag=select_class_shaman] run data remove entity @s interaction

execute as @e[tag=to_items_shaman] on target run function maniac:menu/maniac_classes/display_class_items_shaman
execute as @e[tag=to_items_shaman] run data remove entity @s interaction

execute as @e[tag=to_main_shaman] on target run function maniac:menu/maniac_classes/display_class_main_shaman
execute as @e[tag=to_main_shaman] run data remove entity @s interaction

execute as @e[tag=to_abilities_shaman] on target run function maniac:menu/maniac_classes/display_class_abilities_shaman
execute as @e[tag=to_abilities_shaman] run data remove entity @s interaction

execute as @e[tag=to_items_back_shaman] on target run function maniac:menu/maniac_classes/display_class_items_shaman
execute as @e[tag=to_items_back_shaman] run data remove entity @s interaction

# ===========================================
# ОБРАБОТКА КЛИКОВ МЕФЕДРОНЩИК
# ===========================================

execute as @e[tag=select_class_dependent] on target run scoreboard players set @s SurvivorClass 9
execute as @e[tag=select_class_dependent] on target run tellraw @s "Вы выбрали МЕФЕДРОНЩИК"
execute as @e[tag=select_class_dependent] run data remove entity @s interaction

execute as @e[tag=to_items_dependent] on target run function maniac:menu/maniac_classes/display_class_items_dependent
execute as @e[tag=to_items_dependent] run data remove entity @s interaction

execute as @e[tag=to_main_dependent] on target run function maniac:menu/maniac_classes/display_class_main_dependent
execute as @e[tag=to_main_dependent] run data remove entity @s interaction

execute as @e[tag=to_abilities_dependent] on target run function maniac:menu/maniac_classes/display_class_abilities_dependent
execute as @e[tag=to_abilities_dependent] run data remove entity @s interaction

execute as @e[tag=to_items_back_dependent] on target run function maniac:menu/maniac_classes/display_class_items_dependent
execute as @e[tag=to_items_back_dependent] run data remove entity @s interaction

# ===========================================
# ОБРАБОТКА КЛИКОВ УЧЕНЫЙ
# ===========================================

execute as @e[tag=select_class_scientist] on target run scoreboard players set @s SurvivorClass 10
execute as @e[tag=select_class_scientist] on target run tellraw @s "Вы выбрали Ученый"
execute as @e[tag=select_class_scientist] run data remove entity @s interaction

execute as @e[tag=to_items_scientist] on target run function maniac:menu/maniac_classes/display_class_items_scientist
execute as @e[tag=to_items_scientist] run data remove entity @s interaction

execute as @e[tag=to_main_scientist] on target run function maniac:menu/maniac_classes/display_class_main_scientist
execute as @e[tag=to_main_scientist] run data remove entity @s interaction

execute as @e[tag=to_abilities_scientist] on target run function maniac:menu/maniac_classes/display_class_abilities_scientist
execute as @e[tag=to_abilities_scientist] run data remove entity @s interaction

execute as @e[tag=to_items_back_scientist] on target run function maniac:menu/maniac_classes/display_class_items_scientist
execute as @e[tag=to_items_back_scientist] run data remove entity @s interaction

# ===========================================
# ОБРАБОТКА КЛИКОВ БАБКА ГРЕННИ
# ===========================================
execute as @e[tag=select_class] on target run scoreboard players set @s ManiacClass 1
execute as @e[tag=select_class] on target run tellraw @s "Вы выбрали Бабку Гренни"
execute as @e[tag=select_class] run data remove entity @s interaction


execute as @e[tag=to_items] on target run function maniac:menu/maniac_classes/display_class_items_grenny
execute as @e[tag=to_items] run data remove entity @s interaction

execute as @e[tag=to_main] on target run function maniac:menu/maniac_classes/display_class_main_grenny
execute as @e[tag=to_main] run data remove entity @s interaction

execute as @e[tag=to_abilities] on target run function maniac:menu/maniac_classes/display_class_abilities_grenny
execute as @e[tag=to_abilities] run data remove entity @s interaction

execute as @e[tag=to_items_back] on target run function maniac:menu/maniac_classes/display_class_items_grenny
execute as @e[tag=to_items_back] run data remove entity @s interaction

# ===========================================
# ОБРАБОТКА КЛИКОВ СМЕРТИ
# ===========================================

execute as @e[tag=select_class_death] on target run scoreboard players set @s ManiacClass 10
execute as @e[tag=select_class_death] on target run tellraw @s "Вы выбрали Смерть"
execute as @e[tag=select_class_death] run data remove entity @s interaction


execute as @e[tag=to_items_death] on target run function maniac:menu/maniac_classes/display_class_items_death
execute as @e[tag=to_items_death] run data remove entity @s interaction

execute as @e[tag=to_main_death] on target run function maniac:menu/maniac_classes/display_class_main_death
execute as @e[tag=to_main_death] run data remove entity @s interaction

execute as @e[tag=to_abilities_death] on target run function maniac:menu/maniac_classes/display_class_abilities_death
execute as @e[tag=to_abilities_death] run data remove entity @s interaction

execute as @e[tag=to_items_back_death] on target run function maniac:menu/maniac_classes/display_class_items_death
execute as @e[tag=to_items_back_death] run data remove entity @s interaction