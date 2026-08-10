#!/bin/bash

# ESTO ES SOLO PARA UBUNTU
cd "$(dirname "$0")"

DEBUG_PORT=5005

# Buscar el PID del proceso que escucha en el puerto 10041
PID=$(sudo ss -lptn 'sport = :10041' | grep -oP 'pid=\K\d+')

# Verificar si se encontró un PID
if [ -z "$PID" ]; then
  echo "No se encontró ningún proceso escuchando en el puerto 10041."
else
  echo "Terminando el proceso con PID $PID que escucha en el puerto 10041."
  # Terminar el proceso
  sudo kill -9 $PID
fi

# Iniciar server.jar en modo debug (queda escuchando el debugger en el puerto 5005,
# sin pausar el arranque). En VSCode: Run and Debug -> "Attach" -> puerto 5005.
echo "Iniciando server.jar en modo debug (puerto $DEBUG_PORT)..."
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address="$DEBUG_PORT" -jar server.jar
