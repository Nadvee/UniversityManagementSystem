import { StudentPage } from '../../core/StudentPage.js';
export class RoutinePage extends StudentPage {
    constructor() { super('routine'); }
    async render() {
        const { content } = this.context;
        const { api, panel, routine, button } = this.ui.bindings(this.api);
        const load = this.watch(() => api('/routine/me'), data => {
            content.innerHTML = panel('Weekly class routine', routine(data), 'Saturday – Thursday', button('Print / Save PDF', 'print'));
        });
        await load();
    }
}
