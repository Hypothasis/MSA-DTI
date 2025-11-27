document.addEventListener('DOMContentLoaded', function () {

    function showFlashMessage(message, type = 'error', duration = 8000) {
        const container = document.getElementById('flash-message');
        const textElement = document.getElementById('flash-message-text');
        
        if (!container || !textElement) return;

        textElement.textContent = message;
        container.className = 'flash-message'; 
        container.classList.add(type); 
        container.classList.remove('hidden');

        setTimeout(() => {
            hideFlashMessage();
        }, duration);
    }

    function hideFlashMessage() {
        const flashMessage = document.getElementById('flash-message');
        flashMessage.classList.add('hidden');
    }
    
    document.querySelector('#flash-message .close-flash').addEventListener('click', () => {
        document.getElementById('flash-message').classList.add('hidden');
    });

    const successMessage = /*[[${successMessage}]]*/ null;
    const errorMessage = /*[[${errorMessage}]]*/ null;

    if (successMessage) {
        showFlashMessage(successMessage, 'success');
    }

    if (errorMessage) {
        showFlashMessage(errorMessage, 'error');
    }

    const hostForm = document.getElementById('host-form');
    const createButton = document.getElementById('hostCreateBtn');

    //#######################################################################
    //###        NOVA LÓGICA DE EXCLUSÃO DE DISPONIBILIDADE             ###
    //#######################################################################

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

    const checkboxToGroupMap = {};
    for (const groupName in availabilityGroups) {
        for (const checkboxName of availabilityGroups[groupName]) {
            checkboxToGroupMap[checkboxName] = groupName;
        }
    }

    hostForm.querySelectorAll('input[type="checkbox"]').forEach(checkbox => {
        checkbox.addEventListener('change', (event) => {
            const changedCheckboxName = event.target.name;
            const isChecked = event.target.checked;

            const groupName = checkboxToGroupMap[changedCheckboxName];
            if (!groupName) {
                return; 
            }

            const partners = availabilityGroups[groupName];
            partners.forEach(partnerName => {
                const partnerCheckbox = hostForm.querySelector(`input[name="${partnerName}"]`);
                if (partnerCheckbox) partnerCheckbox.checked = isChecked;
            });

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
        
        createButton.addEventListener('click', async function(event) {
            event.preventDefault();

            const formData = new FormData(hostForm);
            
            const enabledMetrics = Array.from(hostForm.querySelectorAll('input[type="checkbox"]:checked')).map(cb => cb.value);
            
            const dataToSend = {
                hostName: formData.get('hostName'),
                hostZabbixID: formData.get('hostZabbixID'),
                hostDescription: formData.get('hostDescription'),
                hostType: formData.get('hostType'),
                enabledMetrics: enabledMetrics,
                
                healthHttpMetric: formData.get('health-http-metric'),
                customHttpMetric: formData.get('custom-http-metric')
            };

            try {
                const response = await fetch('/admin/api/hosts', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(dataToSend)
                });

                const result = await response.json();

                if (!response.ok) {
                    throw new Error(result.error || `Erro ${response.status}`);
                }

                showFlashMessage(`Host '${result.name}' criado com sucesso!`, 'success');
                hostForm.reset(); 

            } catch (error) {
                console.error('Erro ao criar host:', error);
                showFlashMessage(error.message, 'error');
            }
        });
    }
})
