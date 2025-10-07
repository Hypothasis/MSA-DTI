document.addEventListener('DOMContentLoaded', function () {
    // Função para mostrar a flash message (pode estar em um arquivo JS separado)
    function showFlashMessage(message, type = 'error', duration = 8000) {
        const container = document.getElementById('flash-message');
        const textElement = document.getElementById('flash-message-text');
        
        if (!container || !textElement) return;

        textElement.textContent = message;
        container.className = 'flash-message'; // Reseta as classes
        container.classList.add(type); // Adiciona 'success' ou 'error'
        container.classList.remove('hidden');

        setTimeout(() => {
            hideFlashMessage();
        }, duration);
    }

    function hideFlashMessage() {
        const flashMessage = document.getElementById('flash-message');
        flashMessage.classList.add('hidden');
    }
    
    // Botão de fechar
    document.querySelector('#flash-message .close-flash').addEventListener('click', () => {
        document.getElementById('flash-message').classList.add('hidden');
    });

    // Pega as mensagens do Model do Spring (enviadas via RedirectAttributes)
    const successMessage = /*[[${successMessage}]]*/ null;
    const errorMessage = /*[[${errorMessage}]]*/ null;

    // Se houver uma mensagem de sucesso, chama a função JS
    if (successMessage) {
        showFlashMessage(successMessage, 'success');
    }

    // Se houver uma mensagem de erro, chama a função JS
    if (errorMessage) {
        showFlashMessage(errorMessage, 'error');
    }

    const hostForm = document.getElementById('host-form');
    const createButton = document.getElementById('hostCreateBtn');

    if (hostForm && createButton) {
        
        // Adiciona um listener no BOTÃO, não no submit do form
        createButton.addEventListener('click', async function(event) {
            event.preventDefault(); // Previne qualquer comportamento padrão do botão
            
            // Coleta os dados do formulário
            const formData = new FormData(hostForm);
            const enabledMetrics = Array.from(hostForm.querySelectorAll('input[type="checkbox"]:checked')).map(cb => cb.value);
            
            const dataToSend = {
                hostName: formData.get('hostName'),
                hostZabbixID: formData.get('hostZabbixID'),
                hostDescription: formData.get('hostDescription'),
                hostType: formData.get('hostType'),
                enabledMetrics: enabledMetrics
            };

            try {
                // Envia os dados para a API RESTful usando POST
                const response = await fetch('/admin/api/hosts', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(dataToSend)
                });

                const result = await response.json();

                if (!response.ok) {
                    // Se a resposta não for 2xx, o backend enviou um erro
                    throw new Error(result.error || `Erro ${response.status}`);
                }

                // SUCESSO!
                showFlashMessage(`Host '${result.name}' criado com sucesso!`, 'success');
                hostForm.reset(); // Limpa o formulário

            } catch (error) {
                // ERRO!
                console.error('Erro ao criar host:', error);
                showFlashMessage(error.message, 'error');
            }
        });
    }
})
