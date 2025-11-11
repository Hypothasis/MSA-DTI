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

    //#######################################################################
    //###        NOVA LÓGICA DE EXCLUSÃO DE DISPONIBILIDADE             ###
    //#######################################################################

    // Define os grupos de checkboxes de disponibilidade
    const availabilityGroups = {
        'health': [
            'disponibilidade-global-health', 
            'disponibilidade-especifica-health'
        ],
        'http': [
            'disponibilidade-global-http-agente', 
            'disponibilidade-especifica-http-agente'
        ],
        'standard': [
            'disponibilidade-global', 
            'disponibilidade-especifica'
        ]
    };

    // Cria um mapa reverso para descobrir a qual grupo um checkbox pertence
    const checkboxToGroupMap = {};
    for (const groupName in availabilityGroups) {
        for (const checkboxName of availabilityGroups[groupName]) {
            checkboxToGroupMap[checkboxName] = groupName;
        }
    }

    // Adiciona um listener de evento a CADA checkbox dentro do formulário
    hostForm.querySelectorAll('input[type="checkbox"]').forEach(checkbox => {
        checkbox.addEventListener('change', (event) => {
            const changedCheckboxName = event.target.name;
            const isChecked = event.target.checked;

            // 4. Verifica se o checkbox alterado pertence a um grupo de disponibilidade
            const groupName = checkboxToGroupMap[changedCheckboxName];
            if (!groupName) {
                return; // Se não for, (ex: 'cpu-uso'), não faz nada.
            }

            // 5. É um checkbox de disponibilidade. Sincroniza o "parceiro".
            // (ex: marcar 'global' também marca 'especifica' e vice-versa)
            const partners = availabilityGroups[groupName];
            partners.forEach(partnerName => {
                const partnerCheckbox = hostForm.querySelector(`input[name="${partnerName}"]`);
                if (partnerCheckbox) partnerCheckbox.checked = isChecked;
            });

            // 6. Se o grupo foi ATIVADO, desativa todos os OUTROS grupos
            if (isChecked) {
                for (const otherGroupName in availabilityGroups) {
                    if (otherGroupName !== groupName) {
                        availabilityGroups[otherGroupName].forEach(otherCheckboxName => {
                            const otherCheckbox = hostForm.querySelector(`input[name="${otherCheckboxName}"]`);
                            if (otherCheckbox) otherCheckbox.checked = false;
                        });
                    }
                }
            }
        });
    });

    if (hostForm && createButton) {
        
        // Adiciona um listener no BOTÃO, não no submit do form
        createButton.addEventListener('click', async function(event) {
            event.preventDefault(); // Previne qualquer comportamento padrão do botão
            
            // Esta linha agora funciona, pois a lógica acima garante
            // que apenas as métricas de um grupo de disponibilidade estarão checadas.
            const formData = new FormData(hostForm);
            
            const enabledMetrics = Array.from(hostForm.querySelectorAll('input[type="checkbox"]:checked')).map(cb => cb.value);
            
            // Agora, o 'formData.get()' funcionará
            const dataToSend = {
                hostName: formData.get('hostName'),
                hostZabbixID: formData.get('hostZabbixID'),
                hostDescription: formData.get('hostDescription'),
                hostType: formData.get('hostType'),
                enabledMetrics: enabledMetrics,
                
                // Coleta os valores dos campos de texto customizados
                healthHttpMetric: formData.get('health-http-metric'),
                customHttpMetric: formData.get('custom-http-metric')
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
