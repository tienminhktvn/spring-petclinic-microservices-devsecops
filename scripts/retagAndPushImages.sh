#!/usr/bin/env bash
set -e

OLD_PREFIX="springcommunity"
NEW_PREFIX="tienminhktvn2"

docker images --format '{{.Repository}}:{{.Tag}}' | \
grep "^${OLD_PREFIX}/" | while read -r IMAGE; do
  NEW_IMAGE="${IMAGE/${OLD_PREFIX}/${NEW_PREFIX}}"

  echo "Retagging $IMAGE -> $NEW_IMAGE"
  docker tag "$IMAGE" "$NEW_IMAGE"

  echo "Pushing $NEW_IMAGE"
  docker push "$NEW_IMAGE"

  echo "---------------------------------------"
done

docker images --format "{{.Repository}}:{{.Tag}}" \
  | grep '^springcommunity/' \
  | xargs -r docker rmi


echo "All detected images retagged and pushed successfully ✅"
