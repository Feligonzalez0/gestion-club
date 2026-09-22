/**
 * Formulario de pagos: al seleccionar una cuota, precarga el importe
 * abonado con el importe de esa cuota. El usuario puede modificarlo
 * despues si corresponde.
 */
document.addEventListener('DOMContentLoaded', function () {
    var cuotaSelect = document.getElementById('cuotaId');
    var importeInput = document.getElementById('importe');

    if (!cuotaSelect || !importeInput) {
        return;
    }

    cuotaSelect.addEventListener('change', function () {
        var opcionSeleccionada = cuotaSelect.options[cuotaSelect.selectedIndex];
        var importe = opcionSeleccionada ? opcionSeleccionada.getAttribute('data-importe') : null;

        if (importe) {
            importeInput.value = importe;
        }
    });
});
