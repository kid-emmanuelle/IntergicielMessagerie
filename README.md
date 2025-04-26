# IntergicielMessagerie – Déploiement Kubernetes (EDA)

Projet de messagerie interne d'entreprise utilisant Spring Boot, Kafka et WebSocket.

## Équipe du projet

- Emmanuelle Ngan Ha NGUYEN
- Maxime LOUVET
- Ewenn PAUTRIC-MORIN
- Noé DELVAUX

## Présentation
Ce projet est une démonstration d’une architecture Event-Driven (EDA) basée sur Spring Boot, Kafka et PostgreSQL, déployée sur un cluster Kubernetes local via Minikube.
L’application expose des endpoints REST et communique de façon asynchrone via Kafka.
Le déploiement inclut :
- L’application Java Spring Boot
- Kafka (avec Zookeeper)
- PostgreSQL

## Prérequis
- Docker
- Minikube
- kubectl
- make
- Maven (pour build local)
- WSL2 (si sous Windows)

## Étapes de déploiement
### 1. Démarrer Minikube
```bash
minikube start
```
### 2. Configurer Docker pour Minikube
- Permet de construire l’image directement dans l’environnement Docker de Minikube :
```bash
eval $(minikube -p minikube docker-env)
```
### 3. Construire l’image Docker de l’application
```bash
make build
```
### 4. (Optionnel) Retagger l'image sans préfixe
```bash
docker tag localhost:5000/intergicielmessagerie:0.0.1-SNAPSHOT intergicielmessagerie:0.0.1-SNAPSHOT
```
### 5. Déployer l’ensemble des ressources Kubernetes
```bash
kubectl apply -f k8s-manifests/
```
### 6. Vérifier le déploiement
```bash
kubectl get all -n eda-app
```
- Dans cette étape, vous devez répéter plusieurs fois jusqu'à ce que vous voyiez les pods à l'état ```Running```. Il faudra beaucoup de temps pour la première fois pour Pod Kafka et Postgres.
- Jusqu'à ce que vous voyiez un état comme ceci :

![image](https://github.com/user-attachments/assets/b83b2ab2-0d8c-41d3-9e42-f61cdb9ba91e)
- Si vous voulez voir tous les services exposés par Minikube :
```bash
minikube service list
```
![image](https://github.com/user-attachments/assets/481c01f3-6409-413b-a953-3a84ef0bfb4c)

### 7. Accéder à l’application depuis le navigateur
```bash
minikube service app-service -n eda-app
```
![image](https://github.com/user-attachments/assets/ef625d0f-0b70-4678-8fea-f9019833446c)

**NOTES** : Toujours accès à l'URL du ```Starting tunnel for service app-service```
- Preuve de fonction :

![image](https://github.com/user-attachments/assets/6b14da7c-d2d3-4c9d-b178-1698a29261e9)

## Structure des fichiers
- ```Dockerfile``` : Build multi-stage pour l’application Java
- ```Makefile``` : Commandes pour builder et pusher l’image
- ```k8s-manifests/``` : Tous les manifestes Kubernetes (namespace, secrets, configmaps, déploiements, services, etc.)

## Astuces & Dépannage
- Si le pod de l’application reste en ImagePullBackOff, assurez-vous d’avoir bien construit l’image dans l’environnement Docker de Minikube (eval ...).
- Pour voir les logs de l’application :
```bash
kubectl logs -n eda-app -l app=intergicielmessagerie
```
- Pour accéder au dashboard Kubernetes :
```bash
minikube dashboard
```

## Arrêter et nettoyer
```bash
kubectl delete namespace eda-app
minikube stop
```

## Auteur
Emmanuelle Ngan Ha NGUYEN / 2025

## Licence
MIT
