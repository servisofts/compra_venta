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
echo -e "${BLUE}[1/2] Apagando servidor local (puerto $PUERTO_LOCAL)...${NC}"

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

# --- 2) Reiniciar el servidor de produccion ---
# Igual que en siles.sh: se mata el proceso del puerto en el servidor remoto y el
# systemd/supervisor lo relanza solo con el server.jar de produccion.
echo -e "${CYAN}[2/2] Reiniciando servidor de produccion en $SSH_HOST (puerto $PUERTO)...${NC}"

REMOTE_KILL_CMD='PID=$(sudo ss -lptn "sport = :'"$PUERTO"'" | grep -oP "pid=\K\d+"); if [ -n "$PID" ]; then sudo kill -9 $PID; echo "Proceso $PID terminado, el servicio deberia relanzarse solo."; else echo "No se encontro ningun proceso escuchando en el puerto '"$PUERTO"'."; fi'

printf '%s\n' "$SUDO_PASS" | ssh -tt "$SSH_HOST" "$REMOTE_KILL_CMD"

echo -e "${GREEN}Listo. Debug local apagado y servidor de produccion reiniciado.${NC}"
