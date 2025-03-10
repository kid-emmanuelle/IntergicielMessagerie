# Messagerie Intra-Entreprise

Projet de messagerie interne d'entreprise utilisant Spring Boot, Kafka et WebSocket.

## Équipe du projet

- Emmanuelle Ngan Ha NGUYEN
- Maxime LOUVET
- Ewenn PAUTRIC-MORIN
- Noé DELVAUX

## Description

Cette application est une messagerie intra-entreprise souveraine permettant aux utilisateurs de communiquer en temps réel. Elle propose deux modes de connexion :

1. **Mode Public** : Les utilisateurs peuvent envoyer et recevoir des messages, tant en broadcast qu'en privé. Ils peuvent également échanger des fichiers en mode privé.
2. **Mode Pseudo-Anonyme** : Les utilisateurs ne peuvent que recevoir des messages broadcast, sans pouvoir envoyer de messages.

## Fonctionnalités

- Envoi de messages texte en diffusion générale (broadcast)
- Messagerie privée entre utilisateurs
- Transfert de fichiers jusqu'à 10 Mo
- Gestion des timeout sur les fichiers
- Liste des utilisateurs connectés
- Historique des messages persisté en base de données

## Architecture technique

L'application est construite avec les technologies suivantes :

- **Backend** : Spring Boot
- **Frontend** : Thymeleaf, Bootstrap, JavaScript
- **Bus de messages** : Apache Kafka
- **WebSocket** : SockJS, STOMP
- **Base de données** : PostgreSQL, Hibernate
- **Conteneurisation** : Docker

## Démarrage rapide

### Prérequis

- Java 17+
- Maven
- Docker et Docker Compose

### Installation

1. Cloner le dépôt :
```bash
git clone https://github.com/kid-emmanuelle/IntergicielMessagerie.git
cd IntergicielMessagerie
```

2. Lancer les services avec Docker Compose :
```bash
docker-compose up -d
```

3. Compiler et démarrer l'application :
```bash
mvn spring-boot:run
```
ou vous pouvez aussi lancer l'application directement avec JetBrains IntelliJ IDEA

4. Accéder à l'application :
```
http://localhost:8080
```

5. Accéder à l'interface d'administration de Kafka :
```
http://localhost:8090
```

## Extensions possibles

1. **Authentification** : Ajout d'un système d'authentification avec Spring Security
2. **Cryptage** : Mise en place du chiffrement des messages privés
3. **Interface mobile** : Développement d'applications mobiles via une API REST

## Licence
MIT
