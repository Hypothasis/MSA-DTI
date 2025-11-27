document.addEventListener('DOMContentLoaded', function () {
    const tabRadios = document.querySelectorAll('.tab-radio');
    const tabPanes = document.querySelectorAll('.tab-pane');

    function switchTab() {
        const selectedRadio = document.querySelector('.tab-radio:checked');
        if (!selectedRadio) return;

        const targetId = selectedRadio.getAttribute('data-tab');

        tabPanes.forEach(pane => {
            const paneId = pane.id.replace('tab-', '');
            if (paneId === targetId) {
                pane.classList.add('active');
            } else {
                pane.classList.remove('active'); 
                
                const checkboxes = pane.querySelectorAll('input[type="checkbox"]');
                
                checkboxes.forEach(checkbox => {
                    checkbox.checked = false;
                });
            }
        });
    }

    tabRadios.forEach(radio => {
        radio.addEventListener('change', switchTab);
    });

    switchTab(); 
});