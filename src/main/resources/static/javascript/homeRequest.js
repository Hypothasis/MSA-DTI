document.addEventListener('DOMContentLoaded', function () {

    // Variavel para o tempo de requisição Assícrono
    const countdownElement = document.getElementById('countDownTime');
    const lastUpdateTimeElement = document.getElementById('lastUpdateTime');
    const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

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

    //#######################################################################
    //###             CONFIGURAÇÃO DOS GRÁFICOS DE POINTS                 ###
    //#######################################################################
})