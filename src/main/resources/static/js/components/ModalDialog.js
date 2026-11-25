import { Component } from '../core/Component.js';
export class ModalDialog extends Component {
    render(title, html) {
        const { $, $$, esc, formData, toast } = this.dependencies;
        const previous = $('#modal');
        previous?.close();
        previous?.remove();
        const dialog = document.createElement('dialog');
        dialog.id = 'modal';
        dialog.innerHTML = '<div class="dialog-head"><h2>' + esc(title) + '</h2><button aria-label="Close dialog" data-close>×</button></div><div class="dialog-scroll dialog-body">' + html + '</div>';
        document.body.append(dialog);
        $('[data-close]', dialog).addEventListener('click', () => dialog.close());
        dialog.addEventListener('click', e => {
            if (e.target === dialog)
                dialog.close();
        });
        dialog.showModal();
        return dialog;
    }
}
