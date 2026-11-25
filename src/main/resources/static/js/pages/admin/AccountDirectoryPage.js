import { AdminPage } from '../../core/AdminPage.js';
export class AccountDirectoryPage extends AdminPage {
    constructor(page) { super(page); }
    async render() {
        const { page, content, ref } = this.context;
        const { $, esc, api, post, toast, table, panel, body, badge, actions, button, input, select, form, courseColumns, modal } = this.ui.bindings(this.api);
        const student = page === 'students';
        let current = 1, q = '', d;
        $('#subtitle').textContent = student ? 'Create student accounts and follow their academic progress.' : 'Manage faculty accounts, departments and assigned sections.';
        const load = this.watch(async () => {
            let result = await api('/admin/' + page + '?' + new URLSearchParams({ q, page: current }));
            if (!result.items.length && current > 1) {
                current = Math.max(1, Math.ceil(result.total / 20));
                result = await api('/admin/' + page + '?' + new URLSearchParams({ q, page: current }));
            }
            return result;
        }, result => {
            d = result;
            content.innerHTML = panel(student ? 'Student directory' : 'Faculty directory', body('<form id="account-search" class="actions"><input class="search-input search-wide" name="q" value="' + esc(q) + '" placeholder="Search name or ID" aria-label="Search name or ID"><button class="btn light">Search</button></form>') + table(d.items, [['ID', r => '<span class="code">' + esc(r.login_id) + '</span>'], ['Name', 'full_name'], ...(student ? [['Subject', 'subject'], ['CGPA', r => r.cgpa == null ? 'N/A' : Number(r.cgpa).toFixed(2)], ['Standing', r => badge(r.status)]] : [['Department', r => esc(r.department || 'Not assigned')]]), ['Account', r => badge(r.active ? 'Active' : 'Inactive')], ['Actions', r => '<div class="actions">' + button('View details', 'account-detail', r.id, 'light') + button(r.active ? 'Deactivate' : 'Activate', 'account-status', r.id, r.active ? 'danger' : 'light') + button('Delete', 'account-delete', r.id, 'danger') + '</div>']]) + '<div class="pagination">' + button('Previous', 'account-prev', '', 'light') + '<span>Page ' + current + ' · ' + d.total + ' records</span>' + button('Next', 'account-next', '', 'light') + '</div>', '', button(student ? 'Add Student' : 'Add Teacher', 'account-add', '', 'gold'));
            form('#account-search', async (data) => { q = data.q; current = 1; await load(); });
            const prev = $('[data-action=account-prev]'), next = $('[data-action=account-next]');
            prev.disabled = current === 1;
            next.disabled = current * 20 >= d.total;
            return d;
        });
        await load();
        actions(async (action, id) => {
            if (action === 'account-prev') {
                current--;
                d = await load();
            }
            if (action === 'account-next') {
                current++;
                d = await load();
            }
            if (action === 'account-add') {
                const dialog = modal(student ? 'Add Student' : 'Add Teacher', '<form id="account-form"><div class="form-grid two">' + input('Name', 'name') + input(student ? 'Student ID' : 'Teacher ID', 'loginId') + (student ? select('Subject', 'subject', ref.departments.map(d => [d.code, d.code + ' · ' + d.name])) : select('Department', 'departmentCode', [['', 'Select department'], ...ref.departments.map(d => [d.code, d.name + ' (' + d.code + ')'])])) + "</div><div class=\"actions layout-1\"><button type=\"submit\" class=\"btn\">Create account</button></div></form>");
                form('#account-form', async (data) => { const r = await post('/admin/' + page, data); dialog.close(); toast(r.message); d = await load(); });
            }
            if (action === 'account-status') {
                const row = d.items.find(r => r.id === Number(id));
                await api('/admin/users/' + id, { method: 'PATCH', body: { active: !row.active } });
                toast('Account updated.');
                d = await load();
            }
            if (action === 'account-delete') {
                const row = d.items.find(r => r.id === Number(id));
                const consequences = student
                    ? 'Their account and enrollments will be removed, and reserved seats will be released.'
                    : 'Their course choices will be removed and assigned sections will become unassigned.';
                const dialog = modal(student ? 'Delete student' : 'Delete teacher', '<p>Delete <strong>' + esc(row.full_name) + ' (' + esc(row.login_id) + ')</strong>?</p><p>' + consequences + '</p><p>This cannot be undone.</p><form id="delete-account-form"><div class="actions layout-7"><button type="button" class="btn light" id="cancel-account-delete">Cancel</button><button type="submit" class="btn danger">Delete account</button></div></form>');
                $('#cancel-account-delete', dialog).addEventListener('click', () => dialog.close());
                form('#delete-account-form', async () => {
                    const result = await api('/admin/' + page + '/' + id, { method: 'DELETE' });
                    dialog.close();
                    toast(result.message);
                    await load();
                });
            }
            if (action === 'account-detail') {
                const path = '/admin/' + page + '/' + id;
                const markup = detail => {
                const p = detail.profile;
                const info = (student ? '' : '<div class="profile-line"><span>Department</span><strong>' + esc(p.department || 'Not assigned') + '</strong></div>') + '<div class="profile-line"><span>Name</span><strong>' + esc(p.full_name) + '</strong></div><div class="profile-line"><span>ID</span><strong>' + esc(p.login_id) + '</strong></div>';
                return info + (student ? '<div class="profile-line"><span>Subject / CGPA</span><strong>' + esc(p.subject) + ' / ' + esc(p.cgpa ?? 'N/A') + "</strong></div><h3 class=\"layout-2\">Confirmed courses</h3>" + table(detail.state.confirmed, courseColumns) + "<h3 class=\"layout-2\">Courses on hold</h3>" + table(detail.state.holds, courseColumns) : "<h3 class=\"layout-2\">Assigned courses</h3>" + table(detail.sections, courseColumns) + "<h3 class=\"layout-2\">Course choices</h3>" + table(detail.choices, [['Code', 'code'], ['Course', 'name'], ['Status', r => badge(r.status)], ['Source', 'source']]));
                };
                const dialog = modal(student ? 'Student details' : 'Teacher details', markup(await api(path)));
                this.watchDialog(dialog, () => api(path), detail => { $('.dialog-body', dialog).innerHTML = markup(detail); });
            }
        });
        return;
    }
}
