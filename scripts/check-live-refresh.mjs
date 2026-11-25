import assert from 'node:assert/strict';
import fs from 'node:fs';
import { BasePage } from '../src/main/resources/static/js/core/BasePage.js';

class TestDocument extends EventTarget {
    hidden = false;
    activeElement = null;
    querySelector() { return null; }
}
globalThis.document = new TestDocument();
globalThis.window = new EventTarget();
const page = new BasePage('test'), polls = [];
page.every = (callback, interval) => { const poll = { callback, interval, stopped: false, stop() { this.stopped = true; } }; polls.push(poll); return poll; };
let rows = [{ id: 1 }, { id: 2 }], paints = 0, reads = 0;
let fields = [];
const root = { innerHTML: '', querySelectorAll() { return fields; } };
page.context.content = root;
const input = (id, value, type = 'text') => ({ id, name: id, tagName: 'INPUT', type, value, checked: false, dispatchEvent() {}, focus() {}, setSelectionRange() {} });
const refresh = page.watch(async () => { reads++; return structuredClone(rows); }, data => { paints++; root.innerHTML = data.map(row => row.id).join(','); fields = [input('filter', '')]; });
await refresh();
assert.equal(polls[0].interval, 1000);
await polls[0].callback();
assert.equal(paints, 1, 'Unchanged data must not rebuild the form.');
fields[0].value = 'typed search';
rows = [{ id: 2 }];
await polls[0].callback();
assert.equal(root.innerHTML, '2', 'Deleted rows must disappear.');
assert.equal(fields[0].value, 'typed search', 'Background updates must preserve a typed search.');
const before = reads;
document.hidden = true;
await polls[0].callback();
assert.equal(reads, before);
document.hidden = false;
rows = [];
window.dispatchEvent(new Event('focus'));
await new Promise(resolve => setImmediate(resolve));
assert.equal(root.innerHTML, '', 'Returning from phpMyAdmin must refresh the view.');
await page.ui.runBusy(async () => { await polls[0].callback(); assert.equal(reads, before + 1, 'Polling must not race with a save.'); });

const formPage = new BasePage('drop'), formPolls = [];
formPage.every = callback => { formPolls.push(callback); return { stop() {} }; };
let selected = ['11', '12'];
let formFields = [];
const formRoot = { querySelectorAll() { return formFields; } };
formPage.context.content = formRoot;
const updateForm = formPage.watch(async () => [...selected], data => {
    formFields = data.map(value => ({ ...input('', value, 'checkbox'), name: 'section' })).concat([{ ...input('reason', ''), tagName: 'TEXTAREA' }]);
});
await updateForm();
formFields[1].checked = true;
formFields[2].value = 'Unsaved drop reason';
selected = ['12'];
await formPolls[0]();
assert.equal(formFields[0].value, '12');
assert.equal(formFields[0].checked, true, 'A surviving selected course must remain selected.');
assert.equal(formFields[1].value, 'Unsaved drop reason', 'Background deletion must not erase a typed reason.');
formPage.destroy();

const detailPage = new BasePage('details'), detailPolls = [];
detailPage.every = callback => { detailPolls.push(callback); return { stop() {} }; };
const dialog = new EventTarget();
Object.assign(dialog, { open: true, isConnected: true, close() { this.open = false; this.dispatchEvent(new Event('close')); } });
detailPage.watchDialog(dialog, async () => { const error = new Error('Removed'); error.status = 404; throw error; }, () => {});
await detailPolls[0]();
assert.equal(dialog.open, false, 'Deleted detail records must close their old view.');
detailPage.destroy();

let release;
const delayed = page.watch(() => new Promise(resolve => { release = resolve; }), () => { root.innerHTML = 'late'; });
const pending = delayed();
await Promise.resolve();
page.destroy();
release([]);
await pending;
assert.notEqual(root.innerHTML, 'late', 'A destroyed page must ignore late responses.');

const retryPage = new BasePage('retry');
retryPage.every = () => ({ stop() {} });
let attempts = 0;
const retry = retryPage.watch(() => { if (++attempts === 1) throw new Error('Synchronous read failure'); return []; }, () => {});
await assert.rejects(retry(), /Synchronous read failure/);
await retry();
assert.equal(attempts, 2, 'A failed read must not permanently block later refreshes.');
retryPage.destroy();

for (const file of ['admin/AdminDashboardPage','admin/AccountDirectoryPage','admin/TeacherRequestsPage','admin/SectionEditorPage','admin/AdvisingControlPage','teacher/TeacherDashboardPage','teacher/CourseSelectionPage','teacher/MyCoursesPage','student/StudentDashboardPage','student/AdvisingPage','student/RoutinePage','student/DropPage']) {
    assert.match(fs.readFileSync('src/main/resources/static/js/pages/' + file + '.js','utf8'), /this\.watch\(/, file + ' has no live data reader');
}
console.log('Live refresh checks passed: every role page, 1-second updates, deleted rows, search preservation, focus refresh, unchanged views, save isolation and teardown.');
