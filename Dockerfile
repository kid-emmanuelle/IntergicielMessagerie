# Étape 1: Construire l'application avec Maven
FROM maven:3.8-openjdk-17 AS builder

# Définir le répertoire de travail
WORKDIR /app

# Copier le pom.xml pour télécharger les dépendances
COPY pom.xml .
# Télécharger les dépendances (optimisation de cache Docker)
# Exécuter une commande qui télécharge les dépendances sans compiler
RUN mvn dependency:go-offline -B

# Copier le reste du code source
COPY src ./src

# Compiler l'application et créer le fat JAR
# -DskipTests pour accélérer le build si les tests unitaires ne sont pas nécessaires dans l'image
RUN mvn package -DskipTests

# Étape 2: Créer l'image d'exécution finale
FROM openjdk:17-jdk-slim

# Définir le répertoire de travail
WORKDIR /app

# Copier le JAR construit depuis l'étape 'builder'
# Le nom du JAR est déduit de l'artifactId et version dans pom.xml
COPY --from=builder /app/target/IntergicielMessagerie-0.0.1-SNAPSHOT.jar app.jar

# Exposer le port sur lequel l'application écoute (par défaut 8080 pour Spring Boot)
EXPOSE 8080

# Point d'entrée pour exécuter l'application
ENTRYPOINT ["java", "-jar", "app.jar"] 