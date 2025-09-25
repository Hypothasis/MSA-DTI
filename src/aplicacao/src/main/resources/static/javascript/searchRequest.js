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
    // 3. FUNÇÕES DINÂMICAS PARA ABRIR E PREENCHER CADA MODAL
    // ===================================================================

    // --- AÇÃO DE LER (READ) ---
    function openReadModal(hostData) {
        // Preenche o modal de leitura com os dados do dataset
        document.getElementById('modal-read-name').textContent = hostData.hostName || 'N/A';
        document.getElementById('modal-read-description').textContent = hostData.hostDescription || 'N/A';
        document.getElementById('modal-read-zabbix-id').textContent = hostData.hostZabbixId || 'N/A';
        document.getElementById('modal-read-public-id').textContent = hostData.hostPublicId || 'N/A';
        document.getElementById('modal-read-db-id').textContent = hostData.hostId || 'N/A';
        document.getElementById('modal-read-type').textContent = hostData.hostType || 'N/A';

        // Preenche a lista de métricas dinamicamente
        const metricsList = readModal.querySelector('ul');
        metricsList.innerHTML = ''; // Limpa a lista antiga
        if (hostData.hostMetrics && hostData.hostMetrics.trim()) {
            const metrics = hostData.hostMetrics.trim().split(', ');
            metrics.forEach(metricName => {
                const li = document.createElement('li');
                li.textContent = metricName;
                metricsList.appendChild(li);
            });
        } else {
            metricsList.innerHTML = '<li>Nenhuma métrica configurada.</li>';
        }
        
        // Mostra o modal
        readModal.classList.add('show');
    }

    // --- AÇÃO DE ATUALIZAR (UPDATE) ---
    async function openUpdateModal(hostData) {
        try {
            // 1. Busca os dados completos do host na sua API
            console.log(hostData)
            const response = await fetch(`/admin/api/hosts/${hostId}`);
            if (!response.ok) throw new Error('Host não encontrado.');
            const hostData = await response.json();

            // 2. Preenche os campos básicos do formulário
            updateForm.querySelector('#modal-update-id').value = hostData.id;
            updateForm.querySelector('#modal-update-name').value = hostData.name;
            updateForm.querySelector('#modal-update-zabbixID').value = hostData.zabbixId;
            updateForm.querySelector('#modal-update-description').value = hostData.description;

            // 3. Seleciona a aba (radio button) correta
            const radioToSelect = updateForm.querySelector(`input[name="hostType"][value="${hostData.type}"]`);
            if (radioToSelect) {
                radioToSelect.checked = true;
                // Dispara o evento 'change' para que a lógica de troca de abas (que deve estar em outro script) seja executada
                radioToSelect.dispatchEvent(new Event('change'));
            }
            
            // 4. Limpa todos os checkboxes do formulário antes de marcar os corretos
            updateForm.querySelectorAll('input[type="checkbox"]').forEach(cb => cb.checked = false);

            // 5. Marca os checkboxes que vieram da API como habilitados
            if (hostData.enabledMetrics) {
                hostData.enabledMetrics.forEach(metricName => {
                    const checkbox = updateForm.querySelector(`input[name="${metricName}"]`);
                    if (checkbox) {
                        checkbox.checked = true;
                    }
                });
            }

            // 6. Finalmente, mostra o modal
            updateModal.classList.add('show');

        } catch (error) {
            console.error('Falha ao carregar dados para edição:', error);
            alert(error.message);
        }
    }

    // --- LÓGICA DE ENVIO DA REQUISIÇÃO PUT ---
    if (updateForm) {
        updateForm.addEventListener('submit', async function(event) {
            event.preventDefault(); // Previne o recarregamento da página

            const hostId = document.getElementById('modal-update-id').value;
            const csrfToken = document.querySelector('input[name="_csrf"]').value;
            const csrfHeaderName = document.querySelector('input[name="_csrf"]').name;
            
            // Coleta todos os dados do formulário
            const formData = new FormData(updateForm);
            const enabledMetrics = [];
            updateForm.querySelectorAll('input[type="checkbox"]:checked').forEach(cb => {
                enabledMetrics.push(cb.name);
            });

            const dataToSend = {
                hostName: formData.get('hostName'),
                hostZabbixID: formData.get('hostZabbixID'),
                hostDescription: formData.get('hostDescription'),
                hostType: formData.get('hostType'),
                enabledMetrics: enabledMetrics
            };

            try {
                // Envia os dados para a API RESTful usando FETCH e o método PUT
                const response = await fetch(`/admin/api/hosts/${hostId}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        [csrfHeaderName]: csrfToken
                    },
                    body: JSON.stringify(dataToSend)
                });

                if (!response.ok) {
                    throw new Error('Falha ao salvar as alterações. Status: ' + response.status);
                }

                alert('Host atualizado com sucesso!');
                window.location.reload(); // Recarrega a página para ver as mudanças

            } catch (error) {
                console.error('Erro ao atualizar host:', error);
                alert(error.message);
            }
        });
    }
    
    // --- AÇÃO DE DELETAR (DELETE) ---
    function openDeleteModal(hostData) {
        const deleteModal = document.getElementById('delete-modal');
        
        // CORRIGIDO: Preenche o nome do host na mensagem de confirmação
        deleteModal.querySelector('#modal-delete-name').textContent = hostData.hostName;
        
        // CORRIGIDO: Guarda o ID do host no PRÓPRIO botão de confirmação usando dataset
        deleteModal.querySelector('#confirm-delete-btn').dataset.hostId = hostData.hostId;

        // Mostra o modal de confirmação
        deleteModal.classList.add('show');
    }

    // --- LÓGICA DE ENVIO DA REQUISIÇÃO DELETE ---
    const confirmDeleteBtn = document.getElementById('confirm-delete-btn');
    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener('click', async function() {
            const hostId = this.dataset.hostId;
            
            // ===================================================================
            // CORREÇÃO: Pegar o token e o nome do header do HTML
            // ===================================================================
             const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
             const csrfHeaderName = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
            // ===================================================================

            try {
                const response = await fetch(`/admin/api/hosts/${hostId}`, {
                    method: 'DELETE',
                    headers: {
                        [csrfHeaderName]: csrfToken
                    }
                });

                if (!response.ok) {
                    throw new Error('Falha ao excluir o host. Verifique as permissões.');
                }

                alert('Host excluído com sucesso!');
                
                const itemToRemove = document.querySelector(`li[data-host-id='${hostId}']`);
                if (itemToRemove) itemToRemove.remove();
                
                closeModal(); // Função que fecha o modal

            } catch (error) {
                console.error('Erro ao excluir host:', error);
                alert(error.message);
            }
        });
    }


    // ===================================================================
    // 4. LISTENER PRINCIPAL (DELEGAÇÃO DE EVENTOS)
    // ===================================================================
    if (hostListContainer) {
        hostListContainer.addEventListener('click', function (event) {
            const button = event.target.closest('button');
            if (!button) return; // Se não clicou em um botão, não faz nada

            const hostItem = button.closest('li[data-host-id]');
            if (!hostItem) return; // Garante que temos o item da lista

            // O objeto dataset contém todos os atributos data-* do elemento
            const hostData = hostItem.dataset;

            if (button.classList.contains('read')) {
                openReadModal(hostData);
            }
            
            if (button.classList.contains('update')) {
                openUpdateModal(hostData);
            }
            
            if (button.classList.contains('delete')) {
                openDeleteModal(hostData);
            }
        });
    }
});