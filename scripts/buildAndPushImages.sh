#!/usr/bin/env bash
set -euo pipefail

OLD_PREFIX="springcommunity"
NEW_PREFIX="tienminhktvn2"

echo "🚀 Building selected Petclinic services..."
./mvnw clean install -P buildDocker \
  -pl spring-petclinic-customers-service,\
spring-petclinic-vets-service,\
spring-petclinic-visits-service,\
spring-petclinic-config-server,\
spring-petclinic-api-gateway \
  -am

echo "🏷️ Retagging and pushing images..."
docker images --format '{{.Repository}}:{{.Tag}}' | \
grep "^${OLD_PREFIX}/" | while read -r IMAGE; do
  NEW_IMAGE="${IMAGE/${OLD_PREFIX}/${NEW_PREFIX}}"

  echo "➡️  $IMAGE → $NEW_IMAGE"
  docker tag "$IMAGE" "$NEW_IMAGE"

  docker push "$NEW_IMAGE"
done

echo "🧹 Cleaning up old images..."
docker images --format "{{.Repository}}:{{.Tag}}" \
  | grep "^${OLD_PREFIX}/" \
  | xargs -r docker rmi

echo "✅ All selected images built, retagged, pushed, and cleaned"
