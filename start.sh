#!/bin/bash

# Cores para deixar o terminal bonito
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}==================================================${NC}"
echo -e "${CYAN}   🎥 MICRO-VIDEO-PROCESSOR - AUTOMATION SCRIPT   ${NC}"
echo -e "${CYAN}==================================================${NC}"

echo -e "${YELLOW}[1/6] 🧹 Limpando ambiente antigo (Containers e Volumes)...${NC}"
sudo docker compose down --volumes --remove-orphans

echo -e "${YELLOW}[2/6] 🏗️  Construindo e iniciando a infraestrutura base...${NC}"
# Sobe tudo com 1 instância inicialmente para garantir que a infra (Kafka/Eureka) suba primeiro
sudo docker compose up -d --build

echo -e "${YELLOW}[3/6] ⏳ Aguardando 30 segundos para o Kafka e Eureka iniciarem...${NC}"
# O Kafka demora um pouco para aceitar comandos de criação de tópico
sleep 30

echo -e "${YELLOW}[4/6] ⚙️  Criando Tópicos no Kafka com 4 Partições...${NC}"
# Cria os tópicos explicitamente para garantir o paralelismo
sudo docker exec micro_kafka kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 4 --topic video-received-topic --if-not-exists
sudo docker exec micro_kafka kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 4 --topic video-resized-topic --if-not-exists
sudo docker exec micro_kafka kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 4 --topic video-watermarked-topic --if-not-exists
sudo docker exec micro_kafka kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 4 --topic video-completed-topic --if-not-exists

echo -e "${YELLOW}[5/6] 🚀 Escalando os Workers (3 de cada)...${NC}"
sudo docker compose up -d --scale resizer=3 --scale watermarker=3 --scale transcoder=3

echo -e "${GREEN}==================================================${NC}"
echo -e "${GREEN}   ✅ SISTEMA PRONTO!   ${NC}"
echo -e "${GREEN}==================================================${NC}"
echo -e "Endpoints:"
echo -e " - Eureka:   http://localhost:8761"
echo -e " - Upload:   http://localhost:8080/api/v1/videos/upload (POST)"
echo -e ""
echo -e "${CYAN}>> Pressione ENTER para ver os logs dos workers (CTRL+C para sair)${NC}"
read

# Mostra os logs coloridos e misturados dos trabalhadores
sudo docker compose logs -f resizer watermarker transcoder