Projet SEOC 3A 2019 - 2020: Application BitTorrent
Groupe : Equipe 2 : Rémi Depreux, Gloria Ej-Jennane, Audrey Lentilhac

1. Lancement de l'application

java -jar mybittorrent.jar [--debug] [--info] fichier.torrent dossier_de_téléchargement/ <-ip=IP_interface>

<file.torrent>         torrent file you would like to download
<download_folder>      download folder
[--debug]              mode debug to see trace
[--info]               mode info to see every second which peers are connected
<-ip=IP_interface>     select your ip interface where your server will be available (ex : -ip=em1 or -ip=lo0, etc...) by default it will be lo0

2. Application testée avec Vuze et QBittorrent

3. Tracker utilisé : opentracker

4. Scénario de test conseillé

Lancer l'application BitTorrent en étant leecher à x% (0 ≤ x < 100) avec plusieurs clients Vuze ouverts sur plusieurs ordinateurs connectés en réseau.
