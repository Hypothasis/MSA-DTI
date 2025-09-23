document.addEventListener('DOMContentLoaded', function() {
    let asideIcon = document.getElementById('asideBtn');

    if(asideIcon){
        asideIcon.addEventListener('click', () => {
            document.querySelector('aside').classList.toggle('expanded')
            document.querySelector('.title').classList.toggle('expanded')
            document.querySelectorAll('.listTitle').forEach((titles) =>{
                titles.classList.toggle('expanded')
            })
        })
    }

})