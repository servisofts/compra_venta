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

# --- Datos del servidor de produccion ---
# Se leen de .siles.env (no versionado en git) o se piden por consola si no existe
ENV_FILE=".siles.env"
if [ -f "$ENV_FILE" ]; then
    # shellcheck disable=SC1090
    source "$ENV_FILE"
else
    echo -e "${YELLOW}No se encontro $ENV_FILE con los datos del servidor.${NC}"
    read -r -p "Host SSH (usuario@ip): " SSH_HOST
fi

PUERTO="${PUERTO:-10041}"

if [ -z "${SUDO_PASS:-}" ]; then
    read -r -s -p "Password de sudo en el servidor: " SUDO_PASS
    echo
fi

# --- 2) Reiniciar el servidor de produccion (compra_venta) ---
# Igual que en siles.sh: se mata el proceso del puerto en el servidor remoto y el
# systemd/supervisor lo relanza solo con el server.jar de produccion.
echo -e "${CYAN}[2/3] Reiniciando servidor de produccion en $SSH_HOST (puerto $PUERTO)...${NC}"

REMOTE_KILL_CMD='PID=$(sudo ss -lptn "sport = :'"$PUERTO"'" | grep -oP "pid=\K\d+"); if [ -n "$PID" ]; then sudo kill -9 $PID; echo "Proceso $PID terminado, el servicio deberia relanzarse solo."; else echo "No se encontro ningun proceso escuchando en el puerto '"$PUERTO"'."; fi'

printf '%s\n' "$SUDO_PASS" | ssh -tt "$SSH_HOST" "$REMOTE_KILL_CMD"

echo -e "${GREEN}Servidor de produccion (compra_venta) reiniciado.${NC}"

# --- 3) Reiniciar el servidor serp (docker-compose) ---
# Ese servidor se maneja con servisofts.sh, que es interactivo: pide elegir el entorno
# (1 = serp) y despues la password de sudo. Se simulan esos inputs igual que en app/siles.sh.
if [ -z "${SERP_SSH_HOST:-}" ] || [ -z "${SERP_REMOTE_DIR:-}" ]; then
    echo -e "${YELLOW}No se encontraron SERP_SSH_HOST / SERP_REMOTE_DIR en $ENV_FILE.${NC}"
    read -r -p "Host SSH del servidor serp (usuario@ip): " SERP_SSH_HOST
    read -r -p "Directorio remoto de serp (ej: ~/servicios/serp): " SERP_REMOTE_DIR
fi

echo -e "${CYAN}[3/3] Reiniciando servidor serp en $SERP_SSH_HOST...${NC}"

printf '1\n%s\n' "$SUDO_PASS" | ssh -tt "$SERP_SSH_HOST" "cd $SERP_REMOTE_DIR && ./servisofts.sh down"
printf '1\n%s\n' "$SUDO_PASS" | ssh -tt "$SERP_SSH_HOST" "cd $SERP_REMOTE_DIR && ./servisofts.sh up -d"

echo -e "${GREEN}Listo. Debug local apagado, produccion de compra_venta y servidor serp reiniciados.${NC}"
