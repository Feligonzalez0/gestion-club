/**
 * Formulario de pagos:
 *  - Al seleccionar una cuota, precarga el importe abonado con el importe de
 *    esa cuota. El usuario puede modificarlo despues si corresponde.
 *  - Si el importe ingresado es menor al de la cuota, al guardar se muestra
 *    el modal de confirmacion generico (confirm-modal.js) avisando que el
 *    pago es menor al valor de la cuota.
 *
 * El modal se engancha por el atributo data-confirm del <form>. Como
 * confirm-modal.js deja pasar el submit cuando form.dataset.confirmed es
 * 'true', aca se mantiene ese flag sincronizado con los datos del
 * formulario: 'true' (se guarda directo) cuando el importe es valido, y sin
 * definir (se muestra el aviso) cuando es menor al de la cuota.
 *
 * En la edicion no hay <select> de cuota (la cuota es fija), por lo que no
 * se pide confirmacion.
 */
document.addEventListener('DOMContentLoaded', function () {
    var cuotaSelect = document.getElementById('cuotaId');
    var importeInput = document.getElementById('importe');

    if (!cuotaSelect || !importeInput) {
        return;
    }

    var form = cuotaSelect.form;
    var esSelect = cuotaSelect.tagName === 'SELECT';

    function importeDeLaCuota() {
        if (!esSelect) {
            return null;
        }
        var opcionSeleccionada = cuotaSelect.options[cuotaSelect.selectedIndex];
        var importe = opcionSeleccionada ? opcionSeleccionada.getAttribute('data-importe') : null;
        return importe ? parseInt(importe, 10) : null;
    }

    function actualizarConfirmacion() {
        if (!form) {
            return;
        }

        var importeCuota = importeDeLaCuota();
        var importePago = parseInt(importeInput.value, 10);

        if (importeCuota !== null && !isNaN(importePago) && importePago < importeCuota) {
            form.dataset.confirm = 'El importe ingresado ($' + importePago + ') es menor al valor de la cuota ($'
                    + importeCuota + '). Aun asi, la cuota quedara marcada como PAGADA. '
                    + '¿Querés registrar el pago de todos modos?';
            delete form.dataset.confirmed;
        } else {
            form.dataset.confirmed = 'true';
        }
    }

    cuotaSelect.addEventListener('change', function () {
        var importe = importeDeLaCuota();

        if (importe !== null) {
            importeInput.value = importe;
        }
        actualizarConfirmacion();
    });

    importeInput.addEventListener('input', actualizarConfirmacion);

    // Estado inicial (p. ej. al volver al formulario tras un error de validacion).
    actualizarConfirmacion();
});