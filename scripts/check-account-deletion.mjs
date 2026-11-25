import assert from 'node:assert/strict';
import { AccountDirectoryPage } from '../src/main/resources/static/js/pages/admin/AccountDirectoryPage.js';

async function directory(kind, rows) {
    const page = new AccountDirectoryPage(kind);
    const content = { innerHTML: '' }, forms = new Map(), nodes = new Map(), requests = [];
    let action, dialog, fail = false;
    Object.assign(page.context, { content, ref: { departments: [] } });
    const bindings = {
        ...page.ui.bindings(),
        $(selector) {
            if (!nodes.has(selector)) nodes.set(selector, { disabled: false, listeners: {}, addEventListener(type, fn) { this.listeners[type] = fn; } });
            return nodes.get(selector);
        },
        form(selector, handler) { forms.set(selector, handler); },
        actions(handler) { action = handler; },
        modal(title, html) { dialog = { title, html, closed: false, close() { this.closed = true; } }; return dialog; },
        toast() {},
        async api(path, options = {}) {
            requests.push({ path, ...options });
            if (options.method === 'DELETE') {
                if (fail) throw new Error('Deletion failed');
                rows.splice(rows.findIndex(row => path.endsWith('/' + row.id)), 1);
                return { message: 'Account deleted.' };
            }
            const url = new URL(path, 'http://test'), q = url.searchParams.get('q');
            const filtered = rows.filter(row => !q || row.login_id.includes(q));
            const start = (Number(url.searchParams.get('page')) - 1) * 20;
            return { items: filtered.slice(start, start + 20), total: filtered.length };
        }
    };
    page.ui.bindings = () => bindings;
    await page.render();
    return { content, requests, forms, nodes, click: (...args) => action(...args), dialog: () => dialog, fail(value) { fail = value; } };
}

for (const kind of ['students', 'teachers']) {
    const prefix = kind === 'students' ? 'S' : 'T';
    const rows = [1, 2].map(id => ({ id, login_id: prefix + id, full_name: '<Account ' + id + '>', active: true }));
    const view = await directory(kind, rows);
    assert.match(view.content.innerHTML, /data-action="account-delete"/);
    await view.click('account-delete', '1');
    assert.equal(view.requests.filter(r => r.method === 'DELETE').length, 0, 'Opening confirmation must not delete');
    view.nodes.get('#cancel-account-delete').listeners.click();
    assert.equal(view.dialog().closed, true);
    assert.equal(rows.length, 2, 'Cancel must keep the account');

    await view.forms.get('#account-search')({ q: prefix + '2' });
    await view.click('account-delete', '2');
    assert.match(view.dialog().html, /&lt;Account 2&gt;/, 'The searched row must be escaped and selected');
    view.fail(true);
    await assert.rejects(view.forms.get('#delete-account-form')(), /Deletion failed/);
    assert.equal(view.dialog().closed, false, 'An error must keep confirmation open');
    assert.equal(rows.length, 2);
    view.fail(false);
    await view.forms.get('#delete-account-form')();
    assert.equal(view.requests.at(-2).path, '/admin/' + kind + '/2');
    assert.equal(view.requests.at(-2).method, 'DELETE');
    assert.equal(view.dialog().closed, true);
    assert.deepEqual(rows.map(r => r.id), [1]);
    assert.match(view.content.innerHTML, /0 records/, 'The directory must reload after deleting');

    const pagedRows = Array.from({ length: 21 }, (_, index) => ({ id: index + 1, login_id: prefix + (index + 1), full_name: 'Account ' + (index + 1), active: false }));
    const paged = await directory(kind, pagedRows);
    await paged.click('account-next');
    assert.match(paged.content.innerHTML, /Page 2/);
    await paged.click('account-delete', '21');
    await paged.forms.get('#delete-account-form')();
    assert.match(paged.content.innerHTML, /Page 1 · 20 records/, 'Deleting the last row must return to the previous page');
}
console.log('Account deletion UI checks passed: confirmation, cancel, errors, searched accounts, DELETE requests, refresh and pagination for both directories.');
