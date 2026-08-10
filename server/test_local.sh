#!/bin/bash

# ESTO ES SOLO PARA UBUNTU
cd "$(dirname "$0")"

# --- Colores para los mensajes de consola ---
BLUE='\033[1;34m'
CYAN='\033[1;36m'
GREEN='\033[1;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # sin color

DEBUG_PORT=5005
PUERTO_LOCAL=10041

# --- 1) Apagar el server.jar local (si quedo uno corriendo de una vez anterior) ---
echo -e "${BLUE}[1/3] Apagando servidor local (puerto $PUERTO_LOCAL)...${NC}"

PID=$(sudo ss -lptn "sport = :$PUERTO_LOCAL" | grep -oP 'pid=\K\d+' || true)

if [ -z "$PID" ]; then
  echo -e "${YELLOW}No se encontró ningún proceso escuchando en el puerto $PUERTO_LOCAL.${NC}"
else
  echo "Terminando el proceso con PID $PID que escucha en el puerto $PUERTO_LOCAL."
  sudo kill -9 "$PID"
  echo -e "${GREEN}Servidor local detenido.${NC}"
fi

# --- Datos del servidor serp ---
# Se leen de .siles.env (no versionado en git) o se piden por consola si no existe
ENV_FILE=".siles.env"
if [ -f "$ENV_FILE" ]; then
    # shellcheck disable=SC1090
    source "$ENV_FILE"
fi

if [ -z "${SERP_SSH_HOST:-}" ] || [ -z "${SERP_REMOTE_DIR:-}" ]; then
    echo -e "${YELLOW}No se encontraron SERP_SSH_HOST / SERP_REMOTE_DIR en $ENV_FILE.${NC}"
    read -r -p "Host SSH del servidor serp (usuario@ip): " SERP_SSH_HOST
    read -r -p "Directorio remoto de serp (ej: ~/servicios/serp): " SERP_REMOTE_DIR
fi

if [ -z "${SUDO_PASS:-}" ]; then
    read -r -s -p "Password de sudo en el servidor: " SUDO_PASS
    echo
fi


# --- 3) Iniciar server.jar local en modo debug ---
# Queda escuchando el debugger en el puerto 5005, sin pausar el arranque.
# En VSCode: Run and Debug -> "Attach" -> puerto 5005.
# Esta linea corre en primer plano (bloquea la terminal mientras el server esta arriba).
echo -e "${CYAN}[3/3] Iniciando server.jar en modo debug (puerto $DEBUG_PORT)...${NC}"
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address="$DEBUG_PORT" -jar server.jar

# --- 2) Reiniciar el servidor serp (docker-compose) ---
# Igual que en test_produccion.sh: servisofts.sh es interactivo, se simulan los inputs
# (1 = entorno serp, despues la password de sudo).
echo -e "${CYAN}[2/3] Reiniciando servidor serp en $SERP_SSH_HOST...${NC}"

printf '1\n%s\n' "$SUDO_PASS" | ssh -tt "$SERP_SSH_HOST" "cd $SERP_REMOTE_DIR && ./servisofts.sh down"
printf '1\n%s\n' "$SUDO_PASS" | ssh -tt "$SERP_SSH_HOST" "cd $SERP_REMOTE_DIR && ./servisofts.sh up -d"

echo -e "${GREEN}Servidor serp reiniciado.${NC}"
