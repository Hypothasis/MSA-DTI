document.addEventListener('DOMContentLoaded', function () {

    // Variavel para o tempo de requisição Assícrono
    const countdownElement = document.getElementById('countDownTime');
    const lastUpdateTimeElement = document.getElementById('lastUpdateTime');
    const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

    //#######################################################################
    //###         FUNÇÕES PARA REQUISIÇÃO DE BUSCA DE SERVIÇOS            ###
    //#######################################################################

    const searchInput = document.getElementById('home-search-input');
    const resultsList = document.getElementById('search-results-list');
    const resultsContainer = document.getElementById('outdoor-search');
    let debounceTimer;

    searchInput.addEventListener('input', function(event) {
        const searchTerm = event.target.value;

        // Cancela o timer anterior para não fazer múltiplas requisições
        clearTimeout(debounceTimer);

        if (searchTerm.length < 2) {
            resultsList.innerHTML = '';
            resultsContainer.classList.remove('expanded');
            return;
        }

        // Cria um novo timer. A busca só acontece após 300ms de inatividade
        debounceTimer = setTimeout(() => {
            fetchHosts(searchTerm);
        }, 300);
    });

    async function fetchHosts(term) {
        try {
            const response = await fetch(`/host/api/search?term=${encodeURIComponent(term)}`);
            if (!response.ok) throw new Error('Erro na busca.');
            
            const hosts = await response.json();
            renderResults(hosts);

        } catch (error) {
            console.error("Falha ao buscar hosts:", error);
            resultsList.innerHTML = '<li>Erro ao buscar. Tente novamente.</li>';
            resultsContainer.classList.add('expanded');
        }
    }

    function renderResults(hosts) {
        resultsList.innerHTML = ''; // Limpa resultados antigos

        if (hosts.length === 0) {
            resultsList.innerHTML = '<li>Nenhum serviço encontrado.</li>';
        } else {
            hosts.forEach(host => {
                const li = document.createElement('li');
                const p = document.createElement('p');
                const a = document.createElement('a');

                p.textContent = host.name;
                a.textContent = 'Acessar';
                a.href = `/host/${host.publicId}`;

                li.appendChild(p);
                li.appendChild(a);
                resultsList.appendChild(li);
            });
        }
        // Mostra ou esconde o container de resultados
        resultsContainer.classList.toggle('expanded', hosts.length > 0);
    }

    //#######################################################################
    //###        FUNÇÕES PARA REQUISIÇÃO ASSÍCRONA PARA BACKEND           ###
    //#######################################################################

    function updateLastRequestTime() {
        // Obtém dados no tempo real
        const now = new Date();

        // Parse para Horas, Minutos e Segundos
        const hour = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        const seconds = String(now.getSeconds()).padStart(2, '0');
        const formattedTime = `${hour}:${minutes}:${seconds}`;

        if (lastUpdateTimeElement) {
            // Modifica o valor no html
            lastUpdateTimeElement.textContent = formattedTime;
        }
    }

    // função assícrona principal para chamar requisição ao backend
    async function startCountDown() {
        const update_trigger = 60; 
        let secs_remaining = update_trigger;

        // Laço de repetição continua
        while (true) {
            try {
                // Modifica a tag para horarío real no html
                if (countdownElement) {
                    countdownElement.textContent = `${secs_remaining}s.`;
                }

                // Se a contagem chega a zero
                // zera o tempo e chama função de requisição ao backend
                if (secs_remaining <= 0) {

                    // Registra no elemento do <html> o tempo da última atualização
                    updateLastRequestTime();

                    // Chama função assícrona para backend
                    await getHomeData();

                    // Atualiza para 60s 
                    secs_remaining = update_trigger;

                    // Atualiza elemento para 60s
                    if (countdownElement) {
                        countdownElement.textContent = `${secs_remaining}s.`;
                    }
                }
                
                secs_remaining--;
                
                await delay(1000);

            } catch (error) {
                console.error("Erro no contador:", error);
                await delay(5000);
            }
        }
    }

    async function getHomeData() {
        console.log("Obtendo dados no Zabbix..")
    }

    startCountDown();

})