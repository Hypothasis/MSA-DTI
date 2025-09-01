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
                    await getHostData();

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

    async function getHostData() {
        console.log("Obtendo dados no Zabbix..")
    }

    startCountDown();

    //#######################################################################
    //###             CONFIGURAÇÃO DOS GRÁFICOS DE POINTS                 ###
    //#######################################################################

    //#######################################################################
    //###        CONFIGURAÇÃO DOS GRÁFICOS USANDO APEXCHARTS.JS           ###
    //#######################################################################

    // 1. DADOS DE EXEMPLO: Valores mais realistas para latência em milissegundos (ms).
    let dates = [
        [new Date('2025-08-09').getTime(), 133],
        [new Date('2025-08-10').getTime(), 139],
        [new Date('2025-08-11').getTime(), 167],
        [new Date('2025-08-12').getTime(), 140],
        [new Date('2025-08-13').getTime(), 137],
        [new Date('2025-08-14').getTime(), 198],
        [new Date('2025-08-15').getTime(), 120],
        [new Date('2025-08-16').getTime(), 170],
        [new Date('2025-08-17').getTime(), 169],
        [new Date('2025-08-18').getTime(), 161],
        [new Date('2025-08-19').getTime(), 200],
        [new Date('2025-08-20').getTime(), 133],
        [new Date('2025-08-21').getTime(), 140],
        [new Date('2025-08-22').getTime(), 154],
        [new Date('2025-08-23').getTime(), 164],
        [new Date('2025-08-24').getTime(), 183],
        [new Date('2025-08-25').getTime(), 179],
        [new Date('2025-08-26').getTime(), 118],
        [new Date('2025-08-27').getTime(), 193],
        [new Date('2025-08-28').getTime(), 148]
    ];

    // 2. Criando a instância do gráfico
    var options = {
            series: [{
                name: 'Tempo de Resposta',
                data: dates
            }],
            chart: {
                type: 'area',
                stacked: false,
                height: 350,
                zoom: {
                    type: 'x',
                    enabled: true,
                    autoScaleYaxis: true
                },
                toolbar: {
                    autoSelected: 'zoom'
                },
                foreColor: '#555' 
            },
            dataLabels: {
                enabled: false
            },
            markers: {
                size: 0,
            },
            title: {
                text: 'Latência da Aplicação',
                align: 'left',
                offsetX: 20
            },
            fill: {
                type: 'gradient',
                gradient: {
                    shade: 'light',
                    shadeIntensity: 1,
                    inverseColors: false,
                    opacityFrom: 0.7,
                    opacityTo: 0.1,
                    stops: [0, 90, 100]
                },
            },
            yaxis: {
                labels: {
                    formatter: function (val) {
                        return (val).toFixed(0) + "ms"; 
                    },
                },
                title: {
                    text: 'Latência (ms)'
                },
            },
            xaxis: {
                type: 'datetime',
            },
            tooltip: {
                shared: false,
                y: {
                    formatter: function (val) {
                        return (val).toFixed(0) + "ms"
                    }
                }
            }
    };

    // 3. Rendenrizando o Gŕafico
    const chart = new ApexCharts(document.querySelector("#responseTimeChart"), options);
    chart.render();
})