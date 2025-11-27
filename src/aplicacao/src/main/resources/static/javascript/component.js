document.addEventListener('DOMContentLoaded', function () {

    let alert_flag = false
    let alert_btn = document.getElementById('alert-btn')

    let fullscreen_btn = document.getElementById('fullscreen-btn')
    const fullscreenElement = document.documentElement;

    //#######################################################################
    //###         FUNÇÕES PARA O BOTÃO DE FULLSCREEN E ALERTSOUND         ###
    //#######################################################################

    if (fullscreen_btn){
        fullscreen_btn.addEventListener('click', () => {
            fullscreen_btn.classList.toggle('actived') 

            if (!document.fullscreenElement) {
                startFullscreen(fullscreenElement);
            } else {
                stopFullscreen();
            }
        })
    }

    if (alert_btn){
        alert_btn.addEventListener('click', () => {
            alert_btn.classList.toggle('actived') 
        
            const alert_icon = alert_btn.querySelector('img');

            if (alert_icon) {
                if (!alert_flag) {
                    alert_icon.src = '/image/icons/sound_black.png'
                    alert_flag = true
                } else {
                    alert_icon.src = '/image/icons/mute_black.png'
                    alert_flag = false
                }
            }
        })
    }

    function startFullscreen(elemento) {
        if (elemento.requestFullscreen) {
            elemento.requestFullscreen();
        } else if (elemento.webkitRequestFullscreen) { // Safari
            elemento.webkitRequestFullscreen();
        } else if (elemento.mozRequestFullScreen) { // Firefox
            elemento.mozRequestFullScreen();
        } else if (elemento.msRequestFullscreen) { // IE/Edge
            elemento.msRequestFullscreen();
        }
    }

    function stopFullscreen() {
        if (document.exitFullscreen) {
            document.exitFullscreen();
        } else if (document.webkitExitFullscreen) { // Safari
            document.webkitExitFullscreen();
        } else if (document.mozCancelFullScreen) { // Firefox
            document.mozCancelFullScreen();
        } else if (document.msExitFullscreen) { // IE/Edge
            document.msExitFullscreen();
        }
    }

})