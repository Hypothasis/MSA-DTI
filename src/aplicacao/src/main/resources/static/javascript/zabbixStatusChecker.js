document.addEventListener('DOMContentLoaded', function () {
    const flashMessage = document.getElementById('flash-message');
    const flashMessageText = document.getElementById('flash-message-text');
    const closeButton = document.getElementById('btn-flash-message');

    /**
     * Função principal que busca o status da conexão com o Zabbix.
     */
    async function checkZabbixStatus() {
        try {
            const response = await fetch('/api/public/status/zabbix');
            
            // Se a API não retornar sucesso (ex: 404), trata como erro de conexão com o MSA
            if (!response.ok) {
                throw new Error('Serviço de status do MSA indisponível.');
            }
            
            const statusData = await response.json();
            
            // Se o último status registrado foi de ERRO
            if (statusData.status === 'ERROR') {
                const timestamp = new Date(statusData.timestamp);
                const formattedTime = timestamp.toLocaleString('pt-BR', {
                    dateStyle: 'short',
                    timeStyle: 'medium'
                });
                const message = `Atenção: A última tentativa de coleta de dados (${formattedTime}) falhou. Os dados podem estar desatualizados. Detalhes: ${statusData.details}`;
                showMessage('error', message);
            } else {
                // Se o status for SUCESSO, garante que a mensagem esteja escondida
                hideMessage();
            }

        } catch (error) {
            console.error("Falha ao verificar status do Zabbix:", error);
            showMessage('error', 'Não foi possível conectar ao sistema de monitoramento. Verifique se o serviço MSA está no ar.');
        }
    }

    /**
     * Mostra a mensagem de flash na tela.
     * @param {string} type - O tipo de mensagem ('error', 'success', 'warning').
     * @param {string} text - O texto a ser exibido.
     */
    function showMessage(type, text) {
        if (!flashMessage || !flashMessageText) return;

        flashMessageText.textContent = text;
        // Limpa classes antigas e adiciona a nova
        flashMessage.className = 'flash-message'; 
        flashMessage.classList.add(type);
        // Mostra a mensagem
        flashMessage.classList.remove('hidden');
    }

    /**
     * Esconde a mensagem de flash.
     */
    function hideMessage() {
        if (!flashMessage) return;
        flashMessage.classList.add('hidden');
    }

    // Adiciona o evento de clique para o botão de fechar
    if (closeButton) {
        closeButton.addEventListener('click', hideMessage);
    }
    
    // Inicia o processo: verifica imediatamente e depois a cada 60 segundos
    checkZabbixStatus();
    setInterval(checkZabbixStatus, 60000); // 60 segundos
});