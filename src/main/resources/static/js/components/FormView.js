import { Component } from '../core/Component.js';
export class FormView extends Component {
    render(id, fn) {
        const { $, $$, esc, formData, toast } = this.dependencies;
        $(id)?.addEventListener('submit', async (e) => {
            e.preventDefault();
            const buttons = $$('button[type=submit],button:not([type])', e.target);
            buttons.forEach(b => b.disabled = true);
            try {
                await this.runBusy(() => fn(formData(e.target), e.target));
            }
            catch (err) {
                toast(err.message, true);
            }
            finally {
                buttons.forEach(b => b.disabled = false);
            }
        });
    }
}
