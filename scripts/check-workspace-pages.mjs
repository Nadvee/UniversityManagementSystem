import assert from 'node:assert/strict';
import fs from 'node:fs';
import { UiKit } from '../src/main/resources/static/js/core/UiKit.js';
import { ApplicationShell } from '../src/main/resources/static/js/components/ApplicationShell.js';
import { AdminDashboardPage } from '../src/main/resources/static/js/pages/admin/AdminDashboardPage.js';
import { TeacherDashboardPage } from '../src/main/resources/static/js/pages/teacher/TeacherDashboardPage.js';
import { StudentDashboardPage } from '../src/main/resources/static/js/pages/student/StudentDashboardPage.js';
import { AdvisingControlPage } from '../src/main/resources/static/js/pages/admin/AdvisingControlPage.js';

const pages = {
    TEACHER: ['dashboard', 'course-selection', 'my-courses'],
    ADMIN: ['dashboard', 'teachers', 'students', 'teacher-requests', 'offerings', 'advising-control'],
    STUDENT: ['dashboard', 'advising', 'routine', 'drop']
};
const allowed = new Set(['index.html', 'login.html']);
const app = { innerHTML: '' };
globalThis.document = { querySelector: () => app, querySelectorAll: () => [] };
const kit = new UiKit();
for (const [role, names] of Object.entries(pages)) {
    const area = role.toLowerCase();
    names.forEach(name => allowed.add(area + '/' + name + '.html'));
    assert.deepEqual(fs.readdirSync('src/main/resources/static/' + area).filter(name => name.endsWith('.html')).sort(), names.map(name => name + '.html').sort());
    new ApplicationShell(kit).render({ role, full_name: 'Test User' }, 'dashboard');
    const nav = app.innerHTML.match(/<nav class="nav-items">([\s\S]*?)<\/nav>/)[1];
    assert.deepEqual([...nav.matchAll(/href="([^"]+)"/g)].map(match => match[1]), names.map(name => '/' + area + '/' + name + '.html'));
}
kit.destroy();

for (const [role, Page] of [['ADMIN', AdminDashboardPage], ['TEACHER', TeacherDashboardPage], ['STUDENT', StudentDashboardPage]]) {
    const page = new Page(), content = { innerHTML: '' }, nodes = new Map();
    Object.assign(page.context, { content, user: { role, full_name: 'Test User', login_id: 'TEST1' }, ref: { semesters: [{ is_current: true, name: 'Current semester' }] } });
    const original = page.ui.bindings();
    page.ui.bindings = () => ({ ...original,
        $(selector) { if (!nodes.has(selector)) nodes.set(selector, { textContent: '' }); return nodes.get(selector); },
        async api(path) {
            if (path === '/notices') return [];
            if (path === '/dashboard/me') return { students: 2, teachers: 1, sections: 3, windows: [], audit: [], assigned: 1, pending: 0, profile: { cgpa: null, status: 'NEW', total_credits_completed: 0, department: 'CSE', semester: 'Current' }, routine: [], selectedCredit: 0 };
            throw new Error('Unexpected request: ' + path);
        }
    });
    page.every = () => {};
    await page.render();
    for (const match of content.innerHTML.matchAll(/href="([^"]+)"/g)) {
        const url = new URL(match[1], 'http://test/' + role.toLowerCase() + '/dashboard.html');
        assert.ok(allowed.has(url.pathname.slice(1)), 'Dashboard links to a removed page: ' + url.pathname);
    }
    page.destroy();
}

const control = new AdvisingControlPage(), content = { innerHTML: '' }, forms = new Map(), nodes = new Map(), calls = [];
let action, dialog;
let windows = [{ id: 1, name: 'Advising session', start_at: '2026-11-24T09:00:00', end_at: '2026-11-24T11:00:00', duration_minutes: 120, status: 'OPEN' }];
Object.assign(control.context, { content, ref: { departments: [] } });
const original = control.ui.bindings();
control.ui.bindings = () => ({ ...original,
    $(selector) { if (!nodes.has(selector)) nodes.set(selector, { addEventListener() {} }); return nodes.get(selector); },
    actions(handler) { action = handler; },
    form(id, handler) { forms.set(id, handler); },
    modal() { dialog = { closed: false, close() { this.closed = true; } }; return dialog; },
    toast() {},
    async api(path, options = {}) {
        calls.push({ path, ...options });
        if (options.method === 'DELETE') { windows = []; return { message: 'Deleted' }; }
        return path === '/admin/windows' ? structuredClone(windows) : [];
    },
    async post(path) { calls.push({ path, method: 'POST' }); windows[0].status = path.endsWith('/close') ? 'CLOSED' : 'OPEN'; return { message: 'Updated' }; }
});
await control.render();
assert.match(content.innerHTML, /Create window/);
assert.match(content.innerHTML, /Open now/);
assert.match(content.innerHTML, /Close now/);
assert.doesNotMatch(content.innerHTML, /Cache|seat tools|Warm up|Reconcile|Check integrity/i);
await action('window-close', '1'); assert.equal(windows[0].status, 'CLOSED');
await action('window-open', '1'); assert.equal(windows[0].status, 'OPEN');
await action('window-delete', '1'); assert.equal(windows.length, 1);
await forms.get('#delete-window-form')(); assert.equal(windows.length, 0); assert.equal(dialog.closed, true);
assert.ok(calls.every(call => !/cache|seat/.test(call.path)));
control.destroy();
console.log('Workspace checks passed: exactly 13 role pages, correct menus and dashboard links, advising controls preserved, cache/seat tools removed.');
