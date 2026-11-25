import assert from 'node:assert/strict';
import { SectionEditorPage } from '../src/main/resources/static/js/pages/admin/SectionEditorPage.js';

async function sectionPage(kind) {
    const page = new SectionEditorPage(kind), nodes = new Map(), requests = [], content = { innerHTML: '' };
    const rows = [{ id: 1, code: 'CSE101', name: 'Course One', section_no: '1', capacity: 40, seats_taken: 3, is_offered: true }, { id: 2, code: 'CSE102', name: 'Course Two', section_no: '1', capacity: 40, seats_taken: 0, is_offered: false }];
    const forms = new Map();
    let handler, fail = false, dialog;
    Object.assign(page.context, { content, ref: { courses: [], rooms: [] } });
    const bindings = {
        ...page.ui.bindings(),
        $(selector) {
            if (!nodes.has(selector)) nodes.set(selector, { value: '', innerHTML: '', listeners: {}, addEventListener(type, fn) { this.listeners[type] = fn; } });
            return nodes.get(selector);
        },
        actions(fn) { handler = fn; },
        form(id, fn) { forms.set(id, fn); },
        modal(title, html) { dialog = { title, html, closed: false, close() { this.closed = true; } }; return dialog; },
        toast() {},
        async api(path, options = {}) {
            requests.push({ path, ...options });
            if (options.method === 'DELETE') {
                if (fail) throw new Error('Delete failed');
                rows.splice(rows.findIndex(row => row.id === Number(path.split('/').at(-1))), 1);
                return { message: 'Offering deleted.' };
            }
            if (options.method === 'PATCH') {
                if (fail) throw new Error('Status save failed');
                const id = Number(path.split('/').at(-2));
                rows.find(row => row.id === id).is_offered = options.body.offered;
                return { message: 'Status saved.' };
            }
            return structuredClone(rows);
        }
    };
    page.ui.bindings = () => bindings;
    await page.render();
    return { content, nodes, requests, rows, forms, dialog: () => dialog, click: (...args) => handler(...args), fail(value) { fail = value; } };
}

const view = await sectionPage('offerings'), table = () => view.nodes.get('#sections').innerHTML;
assert.equal((table().match(/data-action="section-status"/g) || []).length, 2, 'Each Offered cell has exactly one status control');
assert.equal((table().match(/data-action="section-edit"/g) || []).length, 2);
assert.equal((table().match(/data-action="section-roster"/g) || []).length, 2);
assert.equal((table().match(/data-action="section-delete"/g) || []).length, 2);
assert.match(table(), /data-action="section-roster"[^>]*>Enrolled Students<\/button><button[^>]*data-action="section-delete"/);
assert.match(table(), /class="badge warn offering-status"[^>]*aria-pressed="true"[^>]*>Enabled<\/button>/);
await view.click('section-status', '1');
assert.deepEqual(view.requests.at(-2), { path: '/admin/offerings/1/status', method: 'PATCH', body: { offered: false } });
assert.match(table(), /data-id="1"[^>]*aria-pressed="false"[^>]*>Disabled<\/button>/);
await view.click('section-status', '1');
assert.deepEqual(view.requests.at(-2).body, { offered: true });
assert.equal(view.rows[0].is_offered, true);

view.nodes.get('#section-filter').value = 'CSE102';
view.nodes.get('#section-filter').listeners.input();
assert.doesNotMatch(table(), /CSE101/);
await view.click('section-status', '2');
assert.deepEqual(view.requests.at(-2).body, { offered: true });
assert.match(view.content.innerHTML, /value="CSE102"/);
assert.doesNotMatch(table(), /CSE101/, 'The current search stays applied after saving');
view.fail(true);
await assert.rejects(view.click('section-status', '2'), /Status save failed/);
assert.equal(view.rows[1].is_offered, true);
assert.match(table(), />Enabled<\/button>/, 'An unsuccessful save does not change the displayed status');
view.fail(false);
await view.click('section-delete', '2');
assert.equal(view.requests.filter(r => r.method === 'DELETE').length, 0);
view.nodes.get('#cancel-section-delete').listeners.click();
assert.equal(view.dialog().closed, true);
assert.equal(view.rows.length, 2);
await view.click('section-delete', '2');
assert.match(view.dialog().html, /CSE102/);
view.fail(true);
await assert.rejects(view.forms.get('#delete-section-form')(), /Delete failed/);
assert.equal(view.dialog().closed, false);
assert.equal(view.rows.length, 2);
view.fail(false);
await view.forms.get('#delete-section-form')();
assert.deepEqual(view.requests.at(-2), { path: '/admin/offerings/2', method: 'DELETE' });
assert.equal(view.dialog().closed, true);
assert.equal(view.rows.length, 1);
assert.match(view.content.innerHTML, /value="CSE102"/);
assert.doesNotMatch(table(), /Course Two/);

const routine = await sectionPage('routine');
assert.doesNotMatch(routine.nodes.get('#sections').innerHTML, /data-action="section-status"/);
assert.doesNotMatch(routine.nodes.get('#sections').innerHTML, /data-action="section-delete"/);
await routine.click('section-status', '1');
assert.equal(routine.requests.filter(r => r.method === 'PATCH').length, 0);
console.log('Offering UI checks passed: status toggles, Enrolled Students, delete, confirmation/cancel, saved state, search preservation and error handling.');
