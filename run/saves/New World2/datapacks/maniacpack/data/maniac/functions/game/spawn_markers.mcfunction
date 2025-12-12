# Удаляем все существующие маркеры с тэгом "removeThis"
kill @e[type=marker,tag=removeThis]
kill @e[type=marker,tag=markerSpawn]

# Спавним новые маркеры на указанных координатах
#Mansion
summon marker -219 33 22 {Tags:["removeThis","brewSpawn"]}
summon marker -258 42 7 {Tags:["removeThis","brewSpawn"]}
summon marker -211 26 17 {Tags:["removeThis","brewSpawn"]}

summon marker -230 40 -4 {Tags:["markerSpawn","plagueMansion"]}
summon marker -258 41 -19 {Tags:["markerSpawn","plagueMansion"]}
summon marker -230 40 -33 {Tags:["markerSpawn","plagueMansion"]}
summon marker -225 32 -26 {Tags:["markerSpawn","plagueMansion"]}
summon marker -255 32 -2 {Tags:["markerSpawn","plagueMansion"]}
summon marker -230 31 4 {Tags:["markerSpawn","plagueMansion"]}
summon marker -235 26 -28 {Tags:["markerSpawn","plagueMansion"]}
summon marker -257 24 -8 {Tags:["markerSpawn","plagueMansion"]}
summon marker -225 24 2 {Tags:["markerSpawn","plagueMansion"]}
summon marker -285 40 -2 {Tags:["markerSpawn","plagueMansion"]}


# FNAF
summon marker -523 45 -25 {Tags:["removeThis","brewSpawn"]}
summon marker -486 45 -140 {Tags:["removeThis","brewSpawn"]}
summon marker -511 44 -49 {Tags:["removeThis","brewSpawn"]}

summon marker -494 44 -7 {Tags:["markerSpawn","plagueFnaf"]}
summon marker -491 45 -66 {Tags:["markerSpawn","plagueFnaf"]}
summon marker -469 44 -23 {Tags:["markerSpawn","plagueFnaf"]}
summon marker -507 45 -38 {Tags:["markerSpawn","plagueFnaf"]}
summon marker -483 57 -73 {Tags:["markerSpawn","plagueFnaf"]}
summon marker -492 45 -122 {Tags:["markerSpawn","plagueFnaf"]}
summon marker -473 45 -134 {Tags:["markerSpawn","plagueFnaf"]}
summon marker -465 45 -60 {Tags:["markerSpawn","plagueFnaf"]}
summon marker -501 45 -88 {Tags:["markerSpawn","plagueFnaf"]}