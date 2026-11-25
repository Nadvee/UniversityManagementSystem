import { Session } from './Session.js';
import { ApiClient } from './ApiClient.js';
import { BaseApi } from '../api/BaseApi.js';
import { UiKit } from './UiKit.js';
import { Poller } from './Poller.js';
export class BasePage {
    #context;
    #session = new Session();
    #ui = new UiKit();
    #api;
    #pollers = [];
    constructor(page) { this.#context = { page }; this.#api = new BaseApi(new ApiClient(this.#session)); }
    get context() { return this.#context; }
    get session() { return this.#session; }
    get ui() { return this.#ui; }
    get api() { return this.#api; }
    async init() {
        try {
            if (await this.guard() === false)
                return;
            this.mountLayout();
            await this.load();
            await this.render();
            this.bind();
            window.addEventListener('pagehide', () => this.destroy(), { once: true });
        }
        catch (error) {
            const { toast, esc, $ } = this.ui.bindings();
            const node = $('#content') || $('#app');
            if (node)
                node.innerHTML = '<div class="alert error">' + esc(error.message) + '</div>';
            toast(error.message, true);
        }
    }
    async guard() { return true; }
    mountLayout() { }
    async load() { }
    async render() { }
    bind() { }
    watch(read, paint, { interval = 1000, root = () => this.context.content, active = () => true, onMissing } = {}) {
        let inFlight, snapshot, poller;
        const refresh = (force = true) => {
            if (this.ui.signal.aborted || !active() || (!force && (document.hidden || this.ui.isBusy))) return Promise.resolve();
            if (inFlight) return force ? inFlight.then(() => refresh(true), () => refresh(true)) : inFlight;
            inFlight = Promise.resolve().then(async () => {
                try {
                    if (!force && this.context.user && this.context.ref) {
                        const [user, reference] = await Promise.all([this.api.currentUser(), this.api.reference()]);
                        if (this.ui.signal.aborted || !active()) return;
                        if (this.requiredRole && user.role !== this.requiredRole) {
                            location.replace(this.session.homePath(user.role));
                            return;
                        }
                        Object.assign(this.context.user, user);
                        Object.assign(this.context.ref, reference);
                    }
                    const data = await read();
                    if (this.ui.signal.aborted || !active() || (!force && this.ui.isBusy)) return;
                    const next = JSON.stringify({ data, user: this.context.user, reference: this.context.ref }, (key, value) => key === 'serverTime' ? undefined : value);
                    if (force || next !== snapshot) {
                        const restore = force ? () => {} : this.preserveView(root());
                        await paint(data);
                        restore();
                        snapshot = next;
                        if (this.context.user) {
                            const name = document.querySelector?.('.user-block strong');
                            if (name) name.textContent = this.context.user.full_name;
                        }
                    }
                    return data;
                } catch (error) {
                    if (force) throw error;
                    if (error.status === 404 && !this.ui.signal.aborted) {
                        if (onMissing) onMissing();
                        else if (this.context.content) this.context.content.innerHTML = '<div class="empty">This record is no longer available.</div>';
                        snapshot = undefined;
                    }
                } finally { inFlight = undefined; }
            });
            return inFlight;
        };
        if (typeof window !== 'undefined') {
            poller = this.every(() => refresh(false), interval);
            window.addEventListener('focus', () => refresh(false), { signal: this.ui.signal });
            document.addEventListener?.('visibilitychange', () => refresh(false), { signal: this.ui.signal });
        }
        refresh.stop = () => poller?.stop();
        return refresh;
    }
    preserveView(root) {
        const fields = [...(root?.querySelectorAll?.('input,select,textarea') || [])];
        const key = (field, index) => field.id || [field.name, field.type, /checkbox|radio/.test(field.type) ? field.value : index].join(':');
        const values = new Map(fields.map((field, index) => [key(field, index), { value: field.value, checked: field.checked }]));
        const focused = fields.indexOf(document.activeElement);
        const selection = focused < 0 ? null : [fields[focused].selectionStart, fields[focused].selectionEnd];
        return () => {
            [...(root?.querySelectorAll?.('input,select,textarea') || [])].forEach((field, index) => {
                const previous = values.get(key(field, index));
                if (!previous) return;
                const changed = field.value !== previous.value;
                if (/checkbox|radio/.test(field.type)) field.checked = previous.checked;
                else if (field.tagName !== 'SELECT' || [...field.options].some(option => option.value === previous.value)) field.value = previous.value;
                if (changed && field.dispatchEvent) field.dispatchEvent(new Event('input', { bubbles: true }));
                if (focused >= 0 && key(field, index) === key(fields[focused], focused)) {
                    field.focus?.({ preventScroll: true });
                    if (selection?.[0] != null) field.setSelectionRange?.(...selection);
                }
            });
        };
    }
    watchDialog(dialog, read, paint) {
        const refresh = this.watch(read, paint, { root: () => dialog, active: () => dialog.open && dialog.isConnected, onMissing: () => dialog.close() });
        dialog.addEventListener?.('close', refresh.stop, { once: true, signal: this.ui.signal });
        return refresh;
    }
    every(fn, ms) { const poller = new Poller().start(fn, ms); this.#pollers.push(poller); return poller; }
    destroy() { this.#pollers.forEach(p => p.stop()); this.#ui.destroy(); }
}
