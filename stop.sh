#!/bin/bash
echo "🛑 Parando sistema e removendo volumes..."
sudo docker compose down --volumes --remove-orphans
echo "✅ Tudo limpo!"