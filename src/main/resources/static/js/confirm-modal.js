/**
 * Modal de confirmación genérico.
 *
 * Reemplaza el uso de onsubmit="return confirm('...')" en cualquier <form>.
 * Basta con agregar el atributo data-confirm="mensaje" al form (ver
 * fragments/confirm-modal.html para el resto de los atributos opcionales).
 *
 * Requiere que la página incluya:
 *   1. El fragment fragments/confirm-modal.html :: confirmModal
 *   2. El bundle de JS de Bootstrap 5 (bootstrap.bundle.min.js)
 *   3. Este archivo
 */
document.addEventListener('DOMContentLoaded', function () {
    var modalEl = document.getElementById('confirmModal');
    if (!modalEl || typeof bootstrap === 'undefined') {
        return;
    }

    var modal = new bootstrap.Modal(modalEl);
    var titleEl = document.getElementById('confirmModalTitleText');
    var bodyEl = document.getElementById('confirmModalBodyText');
    var acceptBtn = document.getElementById('confirmModalAcceptBtn');

    var topIconContainer = document.getElementById('topIconContainer');
    var iconWarning = document.getElementById('iconWarning');
    var iconSuccess = document.getElementById('iconSuccess');

    var defaultTitleHtml = titleEl.innerHTML;
    var defaultIconHtml = topIconContainer.innerHTML;

    var pendingForm = null;

    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            // Si ya fue confirmado por el modal, dejamos pasar el submit real.
            if (form.dataset.confirmed === 'true') {
                return;
            }

            event.preventDefault();
            pendingForm = form;

            bodyEl.textContent = form.dataset.confirm;

            // Título personalizado
            if (form.dataset.confirmTitle) {
                titleEl.textContent = form.dataset.confirmTitle;
            } else {
                titleEl.innerHTML = defaultTitleHtml;
            }

            // Botón de confirmación
            acceptBtn.textContent = form.dataset.confirmButton || 'Confirmar';
            acceptBtn.className = 'btn ' + (form.dataset.confirmVariant || 'btn-danger');

            // Ícono personalizado
            if (form.dataset.confirmIcon) {
                topIconContainer.innerHTML =
                    '<i class="bi ' + form.dataset.confirmIcon + ' fs-2"></i>';
            } else {
                // Restaurar los íconos originales.
                topIconContainer.innerHTML = defaultIconHtml;
            }

            modal.show();
        });
    });

    acceptBtn.addEventListener('click', function () {
        if (!pendingForm) {
            return;
        }

        pendingForm.dataset.confirmed = 'true';
        modal.hide();
        pendingForm.submit();
        pendingForm = null;
    });

    // Si el usuario cancela (Cancelar, X, click afuera, Esc),
    // restauramos el modal a su estado original.
    modalEl.addEventListener('hidden.bs.modal', function () {
        pendingForm = null;

        titleEl.innerHTML = defaultTitleHtml;
        topIconContainer.innerHTML = defaultIconHtml;
    });
});
