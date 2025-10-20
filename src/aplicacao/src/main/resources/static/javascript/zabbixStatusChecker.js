document.addEventListener('DOMContentLoaded', function () {
    const flashMessage = document.getElementById('flash-message');
    const flashMessageText = document.getElementById('flash-message-text');
    const closeButton = document.getElementById('btn-flash-message');

    /**
     * Função principal que realiza um teste de conexão em tempo real com o Zabbix.
     */
    async function checkZabbixStatus() {
        try {
            const response = await fetch('/api/public/zabbix/health-check');
                        
            // Se a resposta for 2xx (ex: 200 OK), a conexão com o Zabbix está boa.
            if (response.ok) {
                hideMessage();
            } else {
                // Se a resposta for 4xx ou 5xx, a conexão com o Zabbix falhou.
                // Vamos tentar ler a mensagem de erro do backend.
                let errorMessage = 'Falha na conexão com o sistema de monitoramento. Os dados podem estar desatualizados.';
                try {
                    const errorData = await response.json();
                    if (errorData && errorData.error) {
                        errorMessage = `Atenção: ${errorData.error}`;
                    }
                } catch (e) {
                    // A resposta de erro não era um JSON. Usamos a mensagem padrão.
                }
                showMessage('error', errorMessage);
            }

        } catch (error) {
            console.error("Falha ao verificar status do Zabbix:", error);
            showMessage('error', 'Não foi possível conectar ao serviço MSA. A aplicação pode estar offline.');
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