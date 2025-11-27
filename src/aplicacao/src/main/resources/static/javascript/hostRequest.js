document.addEventListener('DOMContentLoaded', function () {

    const countdownElement = document.getElementById('countDownTime');
    const lastUpdateTimeElement = document.getElementById('lastUpdateTime');
    const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

    //#######################################################################
    //###        FUNÇÕES PARA REQUISIÇÃO ASSÍCRONA PARA BACKEND           ###
    //#######################################################################

    function updateLastRequestTime() {
        const now = new Date();

        const hour = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        const seconds = String(now.getSeconds()).padStart(2, '0');
        const formattedTime = `${hour}:${minutes}:${seconds}`;

        if (lastUpdateTimeElement) {
            lastUpdateTimeElement.textContent = formattedTime;
        }
    }

    async function startCountDown() {
        const update_trigger = 60; 
        let secs_remaining = update_trigger;

        while (true) {
            try {
                if (countdownElement) {
                    countdownElement.textContent = `${secs_remaining}s.`;
                }

                if (secs_remaining <= 0) {

                    updateLastRequestTime();

                    await getHostData();

                    secs_remaining = update_trigger;

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

    let latencyData = [
        [new Date('2025-08-09T08:10:00').getTime(), 133],
        [new Date('2025-08-10T09:25:00').getTime(), 139],
        [new Date('2025-08-11T10:30:00').getTime(), 167],
        [new Date('2025-08-12T11:45:00').getTime(), 140],
        [new Date('2025-08-13T12:00:00').getTime(), 137],
        [new Date('2025-08-14T13:15:00').getTime(), 198],
        [new Date('2025-08-15T14:30:00').getTime(), 120],
        [new Date('2025-08-16T15:40:00').getTime(), 170],
        [new Date('2025-08-17T16:55:00').getTime(), 169],
        [new Date('2025-08-18T17:00:00').getTime(), 161],
        [new Date('2025-08-19T18:20:00').getTime(), 200],
        [new Date('2025-08-20T19:30:00').getTime(), 133],
        [new Date('2025-08-21T20:45:00').getTime(), 140],
        [new Date('2025-08-22T21:50:00').getTime(), 154],
        [new Date('2025-08-23T22:00:00').getTime(), 164],
        [new Date('2025-08-24T23:10:00').getTime(), 183],
        [new Date('2025-08-25T10:30:00').getTime(), 179],
        [new Date('2025-08-26T11:45:00').getTime(), 118],
        [new Date('2025-08-27T12:00:00').getTime(), 193],
        [new Date('2025-08-28T13:10:00').getTime(), 148]
    ];

    var options = {
        series: [{
            name: 'Tempo de Resposta',
            data: latencyData
        }],
        chart: {
            type: 'area',
            height: 350,
            zoom: { enabled: true },
            toolbar: { autoSelected: 'zoom' },
            foreColor: '#555' 
        },
        colors: ['#452063ff'],
        dataLabels: { enabled: false },
        markers: { size: 0 },
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
                opacityFrom: 0.7,
                opacityTo: 0.1,
                stops: [0, 90, 100]
            },
        },
        yaxis: {
            labels: {
                formatter: function (val) {
                    return (val).toFixed(0) + " ms"; 
                },
            },
            title: {
                text: 'Latência (ms)'
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
                    return (val).toFixed(0) + " ms"
                }
            }
        }
    };

    const responseTimeChart = new ApexCharts(document.querySelector("#responseTimeChart"), options);
    responseTimeChart.render();


    const memoriaEmGb = [95.65, 64.09, 22.00]; 
    const memoriaPorcents = [100, 67, 23];

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
        colors: ['#ad3d24ff'],
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
                    return (val).toFixed(2) + " %";
                }
            }
        }
    };

    const cpuChart = new ApexCharts(document.querySelector("#cpuChart"), options);
    cpuChart.render();


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