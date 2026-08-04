#!/bin/bash
# Agrega, commitea, sincroniza con origin/main y sube los cambios de este proyecto (server/).
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

MSG="${1:-update}"

git add -A -- .

if git diff --cached --quiet; then
    echo "No hay cambios para commitear."
else
    git commit -m "$MSG"
fi

echo "Sincronizando con origin/main..."
git fetch origin
git merge origin/main --no-edit

echo "Subiendo cambios..."
git push origin main

echo "Push completado."
