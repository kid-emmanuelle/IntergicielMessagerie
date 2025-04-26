# Guide d'utilisation du projet TPKubernetes

Ce projet est un service web REST simple développé avec Java Spring Boot. Ce guide vous aidera à tester et exécuter le projet étape par étape.

## Prérequis

- Java 17 ou version supérieure
- Maven
- Docker et Docker Compose

## Étapes pour tester et exécuter le projet

### 1. Cloner le dépôt

Si vous n'avez pas encore cloné le dépôt, utilisez la commande suivante :

```bash
git clone <URL_DU_DEPOT>
cd TPKubernetes
```

### 2. Compiler le projet

Pour compiler le projet et créer un fat JAR, exécutez la commande suivante :

```bash
mvn clean package
```

### 3. Exécuter le projet localement

Pour tester le service localement sans Docker, vous pouvez exécuter la classe principale de l'application :

```bash
mvn spring-boot:run
```

Le service sera accessible à l'adresse suivante :
- **GET**: `http://localhost:8080/monservice/echo/{nom}`
- **POST**: `http://localhost:8080/monservice/hello` avec un corps JSON `{"nom": "value"}`

### 4. Exécuter le projet avec Docker

#### a. Utiliser le Dockerfile simple

Pour construire et exécuter le conteneur avec le Dockerfile simple, utilisez la commande suivante :

```bash
docker-compose up -d
```

Le service sera accessible à l'adresse suivante :
- **GET**: `http://localhost:8081/monservice/echo/{nom}`
- **POST**: `http://localhost:8081/monservice/hello` avec un corps JSON `{"nom": "value"}`

#### b. Utiliser le Dockerfile multi-stage

Pour construire et exécuter le conteneur avec le Dockerfile multi-stage, modifiez le fichier `docker-compose.yml` pour utiliser `Dockerfile.multistage` :

```yaml
services:
  app:
    build:
      context: .
      dockerfile: Dockerfile.multistage
    ports:
      - "8081:8080"
```

Puis, exécutez à nouveau :

```bash
docker-compose up -d
```

### 5. Tester le service

Vous pouvez tester le service en utilisant un outil comme Postman ou cURL.

- **GET**:
```bash
curl http://localhost:8081/monservice/echo/votre_nom
```

- **POST**:
```bash
curl -X POST http://localhost:8081/monservice/hello -H "Content-Type: application/json" -d '{"nom": "votre_nom"}'
```

## Conclusion

Vous avez maintenant un service web REST fonctionnel exécuté avec Java Spring Boot, et vous avez appris à le tester et à l'exécuter à la fois localement et dans un conteneur Docker. Si vous avez des questions, n'hésitez pas à consulter la documentation de Spring Boot ou Docker.
