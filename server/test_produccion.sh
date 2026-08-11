#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

# --- Colores para los mensajes de consola ---
BLUE='\033[1;34m'
CYAN='\033[1;36m'
GREEN='\033[1;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # sin color

PUERTO_LOCAL=10041

# --- 1) Apagar el server.jar local (el que se levanto en modo debug con test_local.sh) ---
echo -e "${BLUE}[1/3] Apagando servidor local (puerto $PUERTO_LOCAL)...${NC}"

PID=$(sudo ss -lptn "sport = :$PUERTO_LOCAL" | grep -oP 'pid=\K\d+' || true)

if [ -z "$PID" ]; then
    echo -e "${YELLOW}No se encontro ningun proceso local escuchando en el puerto $PUERTO_LOCAL.${NC}"
else
    echo "Terminando el proceso local con PID $PID..."
    sudo kill -9 "$PID"
    echo -e "${GREEN}Servidor local detenido.${NC}"
fi

# --- Datos de los servidores ---
# Se leen de .siles.env (no versionado en git) o se piden por consola si falta algo
ENV_FILE=".siles.env"
if [ -f "$ENV_FILE" ]; then
    # shellcheck disable=SC1090
    source "$ENV_FILE"
fi

if [ -z "${SSH_HOST:-}" ] || [ -z "${CV_REMOTE_DIR:-}" ]; then
    echo -e "${YELLOW}No se encontraron SSH_HOST / CV_REMOTE_DIR en $ENV_FILE.${NC}"
    read -r -p "Host SSH de compra_venta (usuario@ip): " SSH_HOST
    read -r -p "Directorio del docker-compose (ej: ~/servicios/compra_venta): " CV_REMOTE_DIR
fi

if [ -z "${SUDO_PASS:-}" ]; then
    read -r -s -p "Password de sudo en el servidor: " SUDO_PASS
    echo
fi

# --- 2) Reiniciar compra_venta (docker-compose) ---
# ~/servicios/compra_venta tiene su propio docker-compose.yaml + servisofts.sh, igual que serp.
# Es interactivo: pide elegir entorno (1 = compra_venta) y despues la password de sudo.
echo -e "${CYAN}[2/3] Reiniciando compra_venta en $SSH_HOST ($CV_REMOTE_DIR)...${NC}"

printf '1\n%s\n' "$SUDO_PASS" | ssh -tt "$SSH_HOST" "cd $CV_REMOTE_DIR && ./servisofts.sh down"
printf '1\n%s\n' "$SUDO_PASS" | ssh -tt "$SSH_HOST" "cd $CV_REMOTE_DIR && ./servisofts.sh up -d"

echo -e "${GREEN}compra_venta reiniciado.${NC}"

# --- 3) Reiniciar el servidor serp (docker-compose) ---
if [ -z "${SERP_SSH_HOST:-}" ] || [ -z "${SERP_REMOTE_DIR:-}" ]; then
    echo -e "${YELLOW}No se encontraron SERP_SSH_HOST / SERP_REMOTE_DIR en $ENV_FILE.${NC}"
    read -r -p "Host SSH del servidor serp (usuario@ip): " SERP_SSH_HOST
    read -r -p "Directorio remoto de serp (ej: ~/servicios/serp): " SERP_REMOTE_DIR
fi

echo -e "${CYAN}[3/3] Reiniciando servidor serp en $SERP_SSH_HOST...${NC}"

printf '1\n%s\n' "$SUDO_PASS" | ssh -tt "$SERP_SSH_HOST" "cd $SERP_REMOTE_DIR && ./servisofts.sh down"
printf '1\n%s\n' "$SUDO_PASS" | ssh -tt "$SERP_SSH_HOST" "cd $SERP_REMOTE_DIR && ./servisofts.sh up -d"

echo -e "${GREEN}Listo. Debug local apagado, compra_venta y serp reiniciados.${NC}"
