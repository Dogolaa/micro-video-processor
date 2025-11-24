#!/bin/bash

# Função para verificar se o comando docker precisa de sudo
docker_cmd="docker"
if ! docker ps > /dev/null 2>&1; then
    echo "Docker precisa de sudo ou não está rodando. Tentando com sudo..."
    docker_cmd="sudo docker"
    # Tenta iniciar o serviço se estiver parado (Docker Nativo)
    if ! sudo docker ps > /dev/null 2>&1; then
         echo "❌ ERRO: O Docker não está rodando. Inicie o Docker Desktop ou rode 'sudo systemctl start docker'."
         exit 1
    fi
fi

# Cores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}=== 🎥 MICRO-VIDEO (SMART LITE MODE) ===${NC}"

# 1. Limpeza
echo -e "${YELLOW}[1/6] 🧹 Limpando ambiente...${NC}"
sudo rm -rf videos_processed
mkdir -p videos_processed
chmod 777 videos_processed

# Usa o comando detectado (docker ou sudo docker)
$docker_cmd compose down --volumes --remove-orphans

# 2. Infraestrutura
echo -e "${YELLOW}[2/6] 🏗️  Subindo Infraestrutura e Serviços (1 de cada)...${NC}"

# HACK DE MEMÓRIA
export JAVA_TOOL_OPTIONS="-Xmx256m"

# Sobe
$docker_cmd compose up -d --build

echo -e "${YELLOW}[3/6] ⏳ Aguardando 40s para estabilização...${NC}"
sleep 40

# 3. Configuração Kafka (Executa dentro do container, então usa o comando base)
echo -e "${YELLOW}[4/6] ⚙️  Configurando Tópicos Kafka (4 partições)...${NC}"
$docker_cmd exec micro_kafka kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 4 --topic video-received-topic --if-not-exists
$docker_cmd exec micro_kafka kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 4 --topic video-resized-topic --if-not-exists
$docker_cmd exec micro_kafka kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 4 --topic video-watermarked-topic --if-not-exists
$docker_cmd exec micro_kafka kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 4 --topic video-completed-topic --if-not-exists

# 4. Escalabilidade
QTD_WORKERS=1

echo -e "${YELLOW}[5/6] 🚀 Verificando escala (Workers: ${QTD_WORKERS})...${NC}"
if [ "$QTD_WORKERS" -gt 1 ]; then
    $docker_cmd compose up -d --scale resizer=$QTD_WORKERS --scale watermarker=$QTD_WORKERS --scale transcoder=$QTD_WORKERS
else
    echo "Mantendo 1 instância de cada para economizar RAM."
fi

echo -e "${GREEN}=== ✅ SISTEMA PRONTO (MODO LITE) ===${NC}"
echo -e "Abra a pasta local 'videos_processed' para ver o resultado."
echo -e "${CYAN}>> Pressione ENTER para ver os logs (CTRL+C para sair)${NC}"
read

$docker_cmd compose logs -f resizer watermarker transcoder