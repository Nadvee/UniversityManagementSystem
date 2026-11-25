import { Toast } from '../components/Toast.js';
import { StatusBadge } from '../components/StatusBadge.js';
import { DataTable } from '../components/DataTable.js';
import { Panel } from '../components/Panel.js';
import { PanelBody } from '../components/PanelBody.js';
import { ActionButton } from '../components/ActionButton.js';
import { StatCard } from '../components/StatCard.js';
import { InputField } from '../components/InputField.js';
import { SelectField } from '../components/SelectField.js';
import { ModalDialog } from '../components/ModalDialog.js';
import { FormValues } from '../components/FormValues.js';
import { FormView } from '../components/FormView.js';
import { ActionHandler } from '../components/ActionHandler.js';
import { RoutineGrid } from '../components/RoutineGrid.js';
export class UiKit {
    #components;
    #controller = new AbortController();
    #pending = 0;
    constructor() { this.#components = { toast: new Toast(this), badge: new StatusBadge(this), table: new DataTable(this), panel: new Panel(this), body: new PanelBody(this), button: new ActionButton(this), metric: new StatCard(this), input: new InputField(this), select: new SelectField(this), modal: new ModalDialog(this), formData: new FormValues(this), form: new FormView(this), actions: new ActionHandler(this), routine: new RoutineGrid(this) }; }
    get signal() { return this.#controller.signal; }
    get isBusy() { return this.#pending > 0; }
    async runBusy(callback) { this.#pending++; try { return await callback(); } finally { this.#pending--; } }
    bindings(api) { const esc = this.escape.bind(this); return { $: this.one.bind(this), $$: this.all.bind(this), esc, fmtDate: this.date.bind(this), api: api?.request.bind(api), post: api?.post.bind(api), courseColumns: [['Course', r => '<span class="code">' + esc(r.code) + '</span><br><span class="muted">' + esc(r.name) + '</span>'], ['Credit', 'credit'], ['Faculty', r => esc(r.faculty || 'To be assigned')], ['Schedule', r => esc(r.schedule || 'Not scheduled')], ['Room', 'room']], ...Object.fromEntries(Object.entries(this.#components).map(([key, component]) => [key, component.render.bind(component)])) }; }
    one(selector, root = document) { return root.querySelector(selector); }
    all(selector, root = document) { return [...root.querySelectorAll(selector)]; }
    escape(value) { return String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
    date(value) { return value ? new Date(value).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' }) : '—'; }
    destroy() { this.#controller.abort(); Object.values(this.#components).forEach(c => c.destroy()); }
}
