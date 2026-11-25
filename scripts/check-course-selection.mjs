import assert from 'node:assert/strict';
import { CourseSelectionPage } from '../src/main/resources/static/js/pages/teacher/CourseSelectionPage.js';
const page = new CourseSelectionPage(), content = { innerHTML: '' }, nodes = new Map(), requests = [];
const controls = [{ disabled: false }, { disabled: false }];
const rows = [null, 'SELECTED', 'CONFIRMED', 'ASSIGNED', 'REJECTED'].map((status, index) => ({ id: index + 1, code: 'CSE10' + (index + 1), name: 'Course ' + (index + 1), credit: 3, department: 'CSE', status, assigned_section_id: status === 'ASSIGNED' ? 9 : null }));
let action, fail = false;
Object.assign(page.context, { content });
const original = page.ui.bindings();
page.ui.bindings = () => ({ ...original,
    $(selector) { if (!nodes.has(selector)) nodes.set(selector, { innerHTML: '', value: '', listeners: {}, addEventListener(type, fn) { this.listeners[type] = fn; } }); return nodes.get(selector); },
    $$() { return controls; },
    actions(handler) { action = handler; }, toast() {},
    async api(path, options = {}) {
        requests.push({ path, ...options });
        if (options.method === 'DELETE') {
            if (fail) throw new Error('Save failed');
            rows.find(row => row.id === Number(path.split('/').at(-1))).status = null;
            return { message: 'Removed' };
        }
        return structuredClone(rows);
    },
    async post(path) {
        requests.push({ path, method: 'POST' });
        if (fail) throw new Error('Save failed');
        const confirming = path.endsWith('/confirm');
        rows.find(row => row.id === Number(path.split('/').at(confirming ? -2 : -1))).status = confirming ? 'CONFIRMED' : 'SELECTED';
        return { message: 'Saved' };
    }
});
await page.render();
const selected = () => nodes.get('#selected-courses').innerHTML, available = () => nodes.get('#available-courses').innerHTML;
assert.ok(content.innerHTML.indexOf('Selected courses') < content.innerHTML.indexOf('Courses offered this semester'));
assert.doesNotMatch(content.innerHTML + selected() + available(), /type="checkbox"|Save selection|Confirm selection|choice-form/);
assert.match(available(), /data-action="choice-select" data-id="1"/);
assert.match(selected(), /data-action="choice-confirm" data-id="2"/);
assert.match(selected(), /data-action="choice-remove" data-id="3"/);
assert.doesNotMatch(selected(), /data-action="choice-remove" data-id="4"/);
nodes.get('#filter').value = 'CSE101'; nodes.get('#filter').listeners.input();
assert.doesNotMatch(available(), /CSE105/); assert.match(selected(), /CSE102/);
await action('choice-select', '1');
assert.deepEqual(requests.at(-2), { path: '/teacher/choices/1', method: 'POST' });
assert.match(selected(), /CSE101/); assert.doesNotMatch(available(), /CSE101/);
await action('choice-confirm', '1');
assert.deepEqual(requests.at(-2), { path: '/teacher/choices/1/confirm', method: 'POST' });
assert.equal(rows[0].status, 'CONFIRMED'); assert.equal(rows[1].status, 'SELECTED');
await action('choice-remove', '1');
assert.deepEqual(requests.at(-2), { path: '/teacher/choices/1', method: 'DELETE' });
assert.doesNotMatch(selected(), /CSE101/); assert.match(available(), /CSE101/);
assert.equal(nodes.get('#filter').value, 'CSE101');
fail = true; await assert.rejects(action('choice-select', '1'), /Save failed/);
assert.match(available(), /CSE101/); assert.doesNotMatch(selected(), /CSE101/);
assert.ok(controls.every(control => !control.disabled));
page.destroy();
console.log('Course selection verified: no checkboxes/footer buttons, separate selected list, per-course Select/Confirm/Remove, saved state, search and errors.');
