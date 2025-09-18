document.addEventListener('DOMContentLoaded', function () {
    /* Filtros na aba Search */
    const filtrosBtn = document.getElementById("filtrosBtn");
    if (filtrosBtn){
        filtrosBtn.addEventListener('click', ()=>{
            document.getElementById('filtrosCats').classList.toggle('actived')
        })
    }    
})
