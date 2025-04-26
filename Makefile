# Nom du registry local
REGISTRY=localhost:5000

# Nom de l'image (basé sur l'artifactId)
IMAGE_NAME=intergicielmessagerie

# Tag de l'image (par exemple, latest ou la version du pom)
IMAGE_TAG=0.0.1-SNAPSHOT

# Nom complet de l'image avec registry
FULL_IMAGE_NAME=$(REGISTRY)/$(IMAGE_NAME):$(IMAGE_TAG)

# Cible par défaut: build et push
all: push

# Construire l'image Docker
build:
	@echo "Construction de l'image $(FULL_IMAGE_NAME)..."
	docker build -t $(FULL_IMAGE_NAME) .

# Pousser l'image vers le registry local
push: build
	@echo "Push de l'image $(FULL_IMAGE_NAME) vers $(REGISTRY)..."
	docker push $(FULL_IMAGE_NAME)

# Cible pour nettoyer (supprimer l'image locale si nécessaire)
clean:
	@echo "Suppression de l'image locale $(FULL_IMAGE_NAME)..."
	docker rmi $(FULL_IMAGE_NAME)

# Indiquer que ces cibles ne sont pas des fichiers
.PHONY: all build push clean 