document.addEventListener('DOMContentLoaded', function () {
    // ===================================================================
    // 1. SELEÇÃO DOS ELEMENTOS DO DOM
    // ===================================================================
    const hostListContainer = document.querySelector('#search article ul');
    
    // Seleciona os modais
    const readModal = document.getElementById('read-modal');
    const updateModal = document.getElementById('update-modal');
    const updateForm = document.getElementById('update-host-form');
    const deleteModal = document.getElementById('delete-modal');
    const allModals = document.querySelectorAll('.modal');

    // Pega o token CSRF das meta tags no <head>
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeaderName = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');


    // ===================================================================
    // 2. LÓGICA PARA FECHAR OS MODAIS
    // ===================================================================

    function closeModal() {
        allModals.forEach(modal => modal.classList.remove('show'));
    }

    document.querySelectorAll('.close-btn').forEach(btn => btn.addEventListener('click', closeModal));
    
    window.addEventListener('click', function(event) {
        if (event.target.classList.contains('modal')) {
            closeModal();
        }
    });

    const cancelDeleteBtn = document.getElementById('cancel-delete-btn');
    if (cancelDeleteBtn) {
        cancelDeleteBtn.addEventListener('click', closeModal);
    }

    // ===================================================================
    // 3. LÓGICA DAS ABAS (TABS)
    // ===================================================================
    function setActiveTab(formElement, hostType) {
        const tabPanes = formElement.querySelectorAll('.tab-pane');
        
        tabPanes.forEach(pane => {
            // O ID do painel é algo como "tab-app" ou "tab-update-app"
            const paneType = pane.id.split('-').pop(); // Pega a última parte do ID
            
            if (paneType === hostType) {
                pane.classList.add('active');
            } else {
                pane.classList.remove('active');
            }
        });
    }


    // ===================================================================
    // 4. FUNÇÕES DINÂMICAS PARA ABRIR E PREENCHER CADA MODAL
    // ===================================================================

    // --- AÇÃO DE LER (READ) ---
    async function openReadModal(hostId) {
        try {
            const response = await fetch(`/admin/api/hosts/${hostId}`);
            if (!response.ok) throw new Error('Host não encontrado.');
            const hostData = await response.json();

            // Preenche o modal de leitura com os dados da API
            readModal.querySelector('#modal-read-name').textContent = hostData.name || 'N/A';
            readModal.querySelector('#modal-read-description').textContent = hostData.description || 'N/A';
            readModal.querySelector('#modal-read-zabbix-id').textContent = hostData.zabbixId || 'N/A';
            readModal.querySelector('#modal-read-public-id').textContent = hostData.publicId || 'N/A';
            readModal.querySelector('#modal-read-db-id').textContent = hostData.id || 'N/A';
            readModal.querySelector('#modal-read-type').textContent = hostData.type || 'N/A';

            const metricsList = readModal.querySelector('ul');
            metricsList.innerHTML = '';
            if (hostData.metrics && hostData.metrics.length > 0) {
                hostData.metrics.forEach(metric => {
                    const li = document.createElement('li');
                    li.textContent = metric.name; // Usa o nome da métrica do objeto
                    metricsList.appendChild(li);
                });
            } else {
                metricsList.innerHTML = '<li>Nenhuma métrica configurada.</li>';
            }
            
            readModal.classList.add('show');
        } catch (error) {
            console.error('Falha ao carregar dados do host:', error);
            alert(error.message);
        }
    }

    // --- AÇÃO DE ATUALIZAR (UPDATE) ---
    async function openUpdateModal(hostId) {
        try {
            const response = await fetch(`/admin/api/hosts/${hostId}`);
            if (!response.ok) throw new Error('Host não encontrado.');
            const hostData = await response.json();
            
            // Preenche os campos básicos do formulário de update
            updateForm.querySelector('#modal-update-id').value = hostData.id;
            updateForm.querySelector('#modal-update-name').value = hostData.name;
            updateForm.querySelector('#modal-update-zabbixID').value = hostData.zabbixId;
            updateForm.querySelector('#modal-update-description').value = hostData.description;

            // Seleciona a aba (radio button) correta
            const radioToSelect = updateForm.querySelector(`input[name="hostType"][value="${hostData.type}"]`);
            if (radioToSelect) radioToSelect.checked = true;
            
            // Chama a função para ativar a aba visualmente
            setActiveTab(updateForm, hostData.type);

            // Limpa todos os checkboxes e marca os corretos
            updateForm.querySelectorAll('input[type="checkbox"]').forEach(cb => cb.checked = false);
            if (hostData.metrics) {
                hostData.metrics.forEach(metric => {
                    // Assume que o 'name' do checkbox é o 'metricKey' da métrica
                    const checkbox = updateForm.querySelector(`input[name="${metric.metricKey}"]`);
                    if (checkbox) checkbox.checked = true;
                });
            }

            updateModal.classList.add('show');
        } catch (error) {
            console.error('Falha ao carregar dados para edição:', error);
            alert(error.message);
        }
    }
    
    // --- AÇÃO DE DELETAR (DELETE) ---
    function openDeleteModal(hostId, hostName) {
        deleteModal.querySelector('#modal-delete-name').textContent = hostName;
        deleteModal.querySelector('#confirm-delete-btn').dataset.hostId = hostId;
        deleteModal.classList.add('show');
    }

    // ===================================================================
    // 5. LISTENERS DE EVENTOS (ACESSO API RESTFUL)
    // ===================================================================

    // Listener para submissão do formulário de UPDATE
    if (updateForm) {
        updateForm.addEventListener('submit', async function(event) {
            event.preventDefault();
            const hostId = document.getElementById('modal-update-id').value;
            
            const formData = new FormData(updateForm);
            const enabledMetrics = Array.from(updateForm.querySelectorAll('input[type="checkbox"]:checked')).map(cb => cb.name);
            
            const dataToSend = {
                hostName: formData.get('hostName'),
                hostZabbixID: formData.get('hostZabbixID'),
                hostDescription: formData.get('hostDescription'),
                hostType: formData.get('hostType'),
                enabledMetrics: enabledMetrics
            };

            try {
                const response = await fetch(`/admin/api/hosts/${hostId}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json', [csrfHeaderName]: csrfToken },
                    body: JSON.stringify(dataToSend)
                });
                if (!response.ok) throw new Error('Falha ao salvar as alterações.');
                alert('Host atualizado com sucesso!');
                window.location.reload();
            } catch (error) {
                alert(error.message);
            }
        });
    }

    // Listener para o botão de confirmação de DELETE
    const confirmDeleteBtn = document.getElementById('confirm-delete-btn');
    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener('click', async function() {
            const hostId = this.dataset.hostId;
            try {
                const response = await fetch(`/admin/api/hosts/${hostId}`, {
                    method: 'DELETE',
                    headers: { [csrfHeaderName]: csrfToken }
                });
                if (!response.ok) throw new Error('Falha ao excluir o host.');
                alert('Host excluído com sucesso!');
                document.querySelector(`li[data-host-id='${hostId}']`)?.remove();
                closeModal();
                window.location.reload();
            } catch (error) {
                alert(error.message);
            }
        });
    }


    // ===================================================================
    // 6. LISTENER PRINCIPAL (OPEN MODAL)
    // ===================================================================
    if (hostListContainer) {
        hostListContainer.addEventListener('click', function (event) {
            const button = event.target.closest('button');
            if (!button) return; // Se não clicou em um botão, não faz nada

            const hostItem = button.closest('li[data-host-id]');
            if (!hostItem) return; // Garante que temos o item da lista

            // O objeto dataset contém todos os atributos data-* do elemento
            const hostData = hostItem.dataset;
            const hostID = hostData.hostId;
            const hostName = hostData.hostName;

            if (button.classList.contains('read')) {
                openReadModal(hostID);
            }
            
            if (button.classList.contains('update')) {
                openUpdateModal(hostID);
            }
            
            if (button.classList.contains('delete')) {
                openDeleteModal(hostID, hostName);
            }
        });
    }
});