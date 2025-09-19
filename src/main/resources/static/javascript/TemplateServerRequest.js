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

    /* CPU */

    let cpuData = [
        [new Date('2025-08-09T10:00:00').getTime(), 23],
        [new Date('2025-08-10T11:15:00').getTime(), 55],
        [new Date('2025-08-11T12:30:00').getTime(), 44],
        [new Date('2025-08-12T13:45:00').getTime(), 23.2],
        [new Date('2025-08-13T14:00:00').getTime(), 25.4],
        [new Date('2025-08-14T15:20:00').getTime(), 23.4],
        [new Date('2025-08-15T16:40:00').getTime(), 33.2],
        [new Date('2025-08-16T17:00:00').getTime(), 44.3],
        [new Date('2025-08-17T18:05:00').getTime(), 55.3],
        [new Date('2025-08-18T19:10:00').getTime(), 23.3],
        [new Date('2025-08-19T20:30:00').getTime(), 26.33],
        [new Date('2025-08-20T21:00:00').getTime(), 67.3],
        [new Date('2025-08-21T22:25:00').getTime(), 23.1],
        [new Date('2025-08-22T23:50:00').getTime(), 22.1],
        [new Date('2025-08-23T08:00:00').getTime(), 22.2],
        [new Date('2025-08-24T09:15:00').getTime(), 33.2],
        [new Date('2025-08-25T10:30:00').getTime(), 33.4],
        [new Date('2025-08-26T11:45:00').getTime(), 26.3],
        [new Date('2025-08-27T12:00:00').getTime(), 34.22],
        [new Date('2025-08-28T13:10:00').getTime(), 24.11]
    ];

    var options = {
        series: [{
            name: 'Uso de CPU',
            data: cpuData
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
        colors: ['#da6247ff'],
        dataLabels: {
            enabled: false
        },
        markers: {
            size: 0,
        },
        title: {
            text: 'Uso de CPU da aplicação',
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
                    return (val).toFixed(0) + " %";
                },
            },
            title: {
                text: 'CPU (%)'
            },
        },
        xaxis: {
            type: 'datetime',
            labels: {
                format: 'dd MMM HH:mm'
            }
        },
        tooltip: {
            shared: false,
            x: {
                format: 'dd MMM yyyy - HH:mm'
            },
            y: {
                formatter: function (val) {
                    // Usei toFixed(2) para mostrar valores decimais se houver
                    return (val).toFixed(2) + " %";
                }
            }
        }
    };

    const cpuChart = new ApexCharts(document.querySelector("#cpuChart"), options);
    cpuChart.render();

    /* Processos CPU */

    let processosAtuais = [
        [new Date('2025-09-18T15:00:00').getTime(), 150],
        [new Date('2025-09-18T15:15:00').getTime(), 155],
        [new Date('2025-09-18T15:30:00').getTime(), 168],
        [new Date('2025-09-18T15:45:00').getTime(), 160],
        [new Date('2025-09-18T16:00:00').getTime(), 163],
        [new Date('2025-09-18T16:15:00').getTime(), 175],
        [new Date('2025-09-18T16:30:00').getTime(), 166]
    ];

    let processosMax = [
        [new Date('2025-09-18T15:00:00').getTime(), 32768],
        [new Date('2025-09-18T15:15:00').getTime(), 32768],
        [new Date('2025-09-18T15:30:00').getTime(), 32768],
        [new Date('2025-09-18T15:45:00').getTime(), 32768],
        [new Date('2025-09-18T16:00:00').getTime(), 32768],
        [new Date('2025-09-18T16:15:00').getTime(), 32768],
        [new Date('2025-09-18T16:30:00').getTime(), 32768]
    ];

    var options = {
        series: [{
            name: 'Número máximo de processos',
            data: processosMax
        }, {
            name: 'Número atual de processos',
            data: processosAtuais
        }],
        chart: {
            height: 350,
            type: 'area',
            zoom: {
                enabled: true
            }
        },
        colors: ['#fb7900ff', '#00e3b2ff'],
        dataLabels: {
            enabled: false
        },
        stroke: {
            curve: 'smooth',
            width: 3
        },
        title: {
            text: 'Uso de Processos ao Longo do Tempo',
            align: 'left'
        },
        yaxis: {
            title: {
                text: ''
            }
        },
        xaxis: {
            type: 'datetime',
            labels: {
                format: 'HH:mm'
            }
        },
        tooltip: {
            x: {
                format: 'dd/MM/yy HH:mm'
            },
        },
        legend: {
            position: 'top'
        }
    };
    
    var processosChart = new ApexCharts(document.querySelector("#cpuProcessosChart"), options);
    processosChart.render();

    /* Troca de Contextos CPU */

    let cpuContextosData = [
        [new Date('2025-08-09T10:00:00').getTime(), 23],
        [new Date('2025-08-10T11:15:00').getTime(), 55],
        [new Date('2025-08-11T12:30:00').getTime(), 44],
        [new Date('2025-08-12T13:45:00').getTime(), 23.2],
        [new Date('2025-08-13T14:00:00').getTime(), 25.4],
        [new Date('2025-08-14T15:20:00').getTime(), 23.4],
        [new Date('2025-08-15T16:40:00').getTime(), 33.2],
        [new Date('2025-08-16T17:00:00').getTime(), 44.3],
        [new Date('2025-08-17T18:05:00').getTime(), 55.3],
        [new Date('2025-08-18T19:10:00').getTime(), 23.3],
        [new Date('2025-08-19T20:30:00').getTime(), 26.33],
        [new Date('2025-08-20T21:00:00').getTime(), 67.3],
        [new Date('2025-08-21T22:25:00').getTime(), 23.1],
        [new Date('2025-08-22T23:50:00').getTime(), 22.1],
        [new Date('2025-08-23T08:00:00').getTime(), 22.2],
        [new Date('2025-08-24T09:15:00').getTime(), 33.2],
        [new Date('2025-08-25T10:30:00').getTime(), 33.4],
        [new Date('2025-08-26T11:45:00').getTime(), 26.3],
        [new Date('2025-08-27T12:00:00').getTime(), 34.22],
        [new Date('2025-08-28T13:10:00').getTime(), 24.11]
    ];

    var options = {
        series: [{
            name: 'Troca de Contextos',
            data: cpuContextosData
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
        colors: ['#da6247ff'],
        dataLabels: {
            enabled: false
        },
        markers: {
            size: 0,
        },
        title: {
            text: 'Troca de contextos no servidor',
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
                    return (val).toFixed(0);
                },
            },
            title: {
                text: ''
            },
        },
        xaxis: {
            type: 'datetime',
            labels: {
                format: 'dd MMM HH:mm'
            }
        },
        tooltip: {
            shared: false,
            x: {
                format: 'dd MMM yyyy - HH:mm'
            },
            y: {
                formatter: function (val) {
                    // Usei toFixed(2) para mostrar valores decimais se houver
                    return (val).toFixed(2);
                }
            }
        }
    };

    const cpuContextosChart = new ApexCharts(document.querySelector("#cpuContextosChart"), options);
    cpuContextosChart.render();

    /* MEMORIA RAM */

    const memoriaEmGb = [95.65, 64.09, 22.00]; // Total, Em Uso, Livre
    const memoriaPorcents = [100, 67, 23]; // Total, Em Uso, Livre

    var options = {
        series: memoriaPorcents,
        chart: {
            height: 390,
            type: 'radialBar',
        },
        plotOptions: {
            radialBar: {
                offsetY: 0,
                startAngle: 0,
                endAngle: 270,
                hollow: {
                    margin: 5,
                    size: '30%',
                    background: 'transparent',
                    image: undefined,
                }
            }
        },
        colors: ['#092d7aff', 'orange', 'green'],
        labels: ['Memória total', 'Memória em Uso', 'Memória Livre'],

        tooltip: {
            enabled: true,
            theme: 'dark',
            y: {
                formatter: function(value, opts) {
                    const gbValue = memoriaEmGb[opts.seriesIndex];
                    return `${value}% (${gbValue.toFixed(1)} GB)`;
                }
            }
        },

        legend: {
            show: true,
            floating: true,
            fontSize: '16px',
            position: 'left',
            offsetX: 50,
            offsetY: 15,
            labels: {
                useSeriesColors: true,
            },
            formatter: function(seriesName, opts) {
                const percentage = opts.w.globals.series[opts.seriesIndex];
                const gbValue = memoriaEmGb[opts.seriesIndex];
                return `${seriesName}: ${percentage}% (${gbValue.toFixed(1)} GB)`;
            }
        },

        responsive: [{
            breakpoint: 480,
            options: {
                legend: {
                    show: false
                }
            }
        }]
    };

    var memoryChart = new ApexCharts(document.querySelector("#memoriaChart"), options);
    memoryChart.render();

    /* SWAP */

    // Dados de SWAP em GB [Total, Em Uso, Livre]
    const swapEmGb = [76.98, 19.74, 57.24]; 

    const swapPorcents = [
        100, 
        parseFloat((swapEmGb[2] / swapEmGb[0] * 100).toFixed(0)),  // % em uso
        parseFloat((swapEmGb[1] / swapEmGb[0] * 100).toFixed(0))   // % livre
    ];

    var options = {
        series: swapPorcents,
        chart: {
            height: 390,
            type: 'radialBar',
        },
        plotOptions: {
            radialBar: {
                offsetY: 0,
                startAngle: 0,
                endAngle: 270,
                hollow: {
                    margin: 5,
                    size: '30%',
                    background: 'transparent',
                    image: undefined,
                }
            }
        },
        // Cores para Total, Usado e Livre (pode ajustar se quiser)
        colors: ['#096d7aff', 'orange', 'green'], 
        
        // Novas legendas para o armazenamento
        labels: ['SWAP Total', 'SWAP em Uso', 'SWAP Livre'],

        tooltip: {
            enabled: true,
            theme: 'dark',
            y: {
                formatter: function(value, opts) {
                    const gbValue = swapEmGb[opts.seriesIndex];
                    return `${value}% (${gbValue.toFixed(1)} GB)`;
                }
            }
        },

        legend: {
            show: true,
            floating: true,
            fontSize: '16px',
            position: 'left',
            offsetX: 50,
            offsetY: 15,
            labels: {
                useSeriesColors: true,
            },
            formatter: function(seriesName, opts) {
                const percentage = opts.w.globals.series[opts.seriesIndex];
                const gbValue = swapEmGb[opts.seriesIndex];
                return `${seriesName}: ${percentage}% (${gbValue.toFixed(1)} GB)`;
            }
        },

        responsive: [{
            breakpoint: 480,
            options: {
                legend: {
                    show: false
                }
            }
        }]
    };

    var swapChart = new ApexCharts(document.querySelector("#swapChart"), options);
    swapChart.render();
 
    /* ARMAZENAMENTO */

    // Dados de armazenamento em GB [Total ("/"), Em Uso ("/"), Total ("/boot"), Em Uso ("/boot")]
    const armazenamentoEmGb = [76.97, 19.39, 0.99, 0.224323273]; 

    var optionsArmazenamento = {
        series: armazenamentoEmGb,
        chart: {
            height: 390,
            type: 'radialBar',
        },
        plotOptions: {
            radialBar: {
                offsetY: 0,
                startAngle: 0,
                endAngle: 270,
                hollow: {
                    margin: 5,
                    size: '30%',
                    background: 'transparent',
                    image: undefined,
                }
            }
        },
        // Cores para Total ("/"), Em Uso ("/"), Total ("/boot"), Em Uso ("/boot")
        colors: ['#4d097aff', 'orange', '#4d097aff','orange'], 
        
        // Novas legendas para o armazenamento
        labels: ['Armazenamento Total ("/")', 'Armazenamento Livre ("/")', 'Armazenamento em Uso ("/boot")', 'Armazenamento Livre ("/boot")'],

        tooltip: {
            enabled: true,
            theme: 'dark',
            y: {
                formatter: function(value, opts) {
                    // Pega o valor correspondente em GB do array de armazenamento
                    const gbValue = armazenamentoEmGb[opts.seriesIndex];
                    return `${value}% (${gbValue.toFixed(1)} GB)`;
                }
            }
        },

        legend: {
            show: true,
            floating: true,
            fontSize: '16px',
            position: 'left',
            offsetX: 50,
            offsetY: 15,
            labels: {
                useSeriesColors: true,
            },
            formatter: function(seriesName, opts) {
                const percentage = opts.w.globals.series[opts.seriesIndex];
                // Pega o valor correspondente em GB do array de armazenamento
                const gbValue = armazenamentoEmGb[opts.seriesIndex];
                return `${seriesName}: ${percentage}% (${gbValue.toFixed(1)} GB)`;
            }
        },

        responsive: [{
            breakpoint: 480,
            options: {
                legend: {
                    show: false
                }
            }
        }]
    };

    var armazenamentoChart = new ApexCharts(document.querySelector("#armazenamentoChart"), optionsArmazenamento);
    armazenamentoChart.render();

    /* Banda Larga de Dados */

    let dadosEnviados = [
        [new Date('2025-09-15T14:00:00').getTime(), 2.5],
        [new Date('2025-09-15T14:15:00').getTime(), 3.1],
        [new Date('2025-09-15T14:30:00').getTime(), 4.0],
        [new Date('2025-09-15T14:45:00').getTime(), 3.5],
        [new Date('2025-09-15T15:00:00').getTime(), 5.2],
        [new Date('2025-09-15T15:15:00').getTime(), 4.8]
    ];

    let dadosRecebidos = [
        [new Date('2025-09-15T14:00:00').getTime(), 10.2],
        [new Date('2025-09-15T14:15:00').getTime(), 12.5],
        [new Date('2025-09-15T14:30:00').getTime(), 15.0],
        [new Date('2025-09-15T14:45:00').getTime(), 11.8],
        [new Date('2025-09-15T15:00:00').getTime(), 18.3],
        [new Date('2025-09-15T15:15:00').getTime(), 16.5]
    ];

    var options = {
        series: [{
            name: 'Dados Enviados (Upload)',
            data: dadosEnviados
        }, {
            name: 'Dados Recebidos (Download)',
            data: dadosRecebidos
        }],
        chart: {
            height: 350,
            type: 'area',
            zoom: {
                enabled: true
            }
        },
        colors: ['#008FFB', '#00E396'],
        dataLabels: {
            enabled: false
        },
        stroke: {
            curve: 'smooth',
            width: 3
        },
        title: {
            text: 'Tráfego de Dados da Aplicação',
            align: 'left'
        },
        yaxis: {
            title: {
                text: 'Tráfego (Mbps)'
            },
            labels: {
                formatter: function(val) {
                    return val.toFixed(2) + " Mbps";
                }
            }
        },
        xaxis: {
            type: 'datetime',
            labels: {
                format: 'HH:mm'
            }
        },
        tooltip: {
            x: {
                format: 'dd/MM/yy HH:mm'
            },
        },
        legend: {
            position: 'top'
        }
    };

    var dataChart = new ApexCharts(document.querySelector("#dadosChart"), options);
    dataChart.render();
})