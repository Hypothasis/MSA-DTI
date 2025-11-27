document.addEventListener('DOMContentLoaded', function () {
    const flashMessage = document.getElementById('flash-message');
    const flashMessageText = document.getElementById('flash-message-text');
    const closeButton = document.getElementById('btn-flash-message');

    async function checkZabbixStatus() {
        try {
            const response = await fetch('/api/public/zabbix/health-check');
                        
            if (response.ok) {
                hideMessage();
            } else {
                let errorMessage = 'Falha na conexão com o sistema de monitoramento. Os dados podem estar desatualizados.';
                try {
                    const errorData = await response.json();
                    if (errorData && errorData.error) {
                        errorMessage = `Atenção: ${errorData.error}`;
                    }
                } catch (e) {
                }
                showMessage('error', errorMessage);
            }

        } catch (error) {
            console.error("Falha ao verificar status do Zabbix:", error);
            showMessage('error', 'Não foi possível conectar ao serviço MSA. A aplicação pode estar offline.');
        }
    }

    function showMessage(type, text) {
        if (!flashMessage || !flashMessageText) return;

        flashMessageText.textContent = text;
        flashMessage.className = 'flash-message'; 
        flashMessage.classList.add(type);
        flashMessage.classList.remove('hidden');
    }

    function hideMessage() {
        if (!flashMessage) return;
        flashMessage.classList.add('hidden');
    }

    if (closeButton) {
        closeButton.addEventListener('click', hideMessage);
    }
    
    checkZabbixStatus();
    setInterval(checkZabbixStatus, 60000);
});