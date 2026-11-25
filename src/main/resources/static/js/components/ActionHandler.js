import { Component } from '../core/Component.js';
export class ActionHandler extends Component {
    #handlers = [];
    #listening = false;
    render(handler) {
        this.#handlers.push(handler);
        if (this.#listening)
            return;
        this.#listening = true;
        document.addEventListener('click', async (event) => {
            const button = event.target.closest('[data-action]');
            if (!button || button.disabled)
                return;
            button.disabled = true;
            try {
                await this.runBusy(async () => {
                    for (const callback of this.#handlers) await callback(button.dataset.action, button.dataset.id, button);
                });
            }
            catch (error) {
                this.dependencies.toast(error.message, true);
            }
            finally {
                button.disabled = false;
            }
        }, { signal: this.signal });
    }
}
