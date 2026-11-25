import { AdminPage } from '../../core/AdminPage.js';
export class AdvisingControlPage extends AdminPage {
    constructor() { super('advising-control'); }
    async render() {
        const { content, ref } = this.context;
        const { $, $$, esc, api, post, toast, table, panel, badge, fmtDate, actions, button, input, select, form, modal } = this.ui.bindings(this.api);
        let windows = [], sections = [];
        const load = this.watch(() => Promise.all([api('/admin/windows'), api('/admin/offerings')]), data => {
            [windows, sections] = data;
            content.innerHTML = panel('Advising windows', table(windows, [['Name', 'name'], ['Starts', r => fmtDate(r.start_at)], ['Ends', r => fmtDate(r.end_at)], ['Duration', r => r.duration_minutes + ' minutes'], ['Status', r => badge(r.status)], ['Actions', r => '<div class="actions">' + button('Open now', 'window-open', r.id) + button('Close now', 'window-close', r.id, 'light') + '<button type="button" class="btn small danger" data-action="window-delete" data-id="' + r.id + '" aria-label="Delete ' + esc(r.name) + '" title="Delete advising window">×</button></div>']]), 'Each window defaults to two hours.', button('Create window', 'window-add', '', 'gold'));
        });
        await load();
        actions(async (action, id) => {
            if (action === 'window-add') {
                const eligible = sections.filter(s => s.teacher_id && s.is_offered);
                const start = new Date(Date.now() - new Date().getTimezoneOffset() * 60000 + 300000).toISOString().slice(0, 16);
                const dialog = modal('Create advising window', '<form id="window-form"><div class="form-grid two">' + input('Window name', 'name') + input('Start date & time', 'startAt', 'datetime-local', start) + input('Duration (minutes)', 'duration', 'number', 120) + select('Filter by department', 'window-dept', [['', 'All departments'], ...ref.departments.map(d => [d.code, d.name])]) + "</div><h3 class=\"layout-5\">Teacher-assigned sections</h3><div class=\"actions layout-6\"><button class=\"btn small light\" id=\"window-all\" type=\"button\">Select visible sections</button></div><div class=\"check-list\" id=\"window-options\">" + eligible.map(s => '<label class="check-item" data-dept="' + esc(s.dept_code) + '"><input type="checkbox" name="section" value="' + s.id + '"> ' + esc(s.code + ' · ' + s.section_no + ' · ' + s.faculty) + '</label>').join('') + '</div><button class="btn" type="submit">Create window</button></form>');
                $('#window-dept').addEventListener("change", e => $$('.check-item').forEach(n => n.hidden = !!e.target.value && n.dataset.dept !== e.target.value));
                $('#window-all').addEventListener("click", () => $$('.check-item:not([hidden]) input').forEach(n => n.checked = true));
                form('#window-form', async (data, f) => { await post('/admin/windows', { name: data.name, startAt: data.startAt, duration: Number(data.duration), sectionIds: $$('[name=section]:checked', f).map(n => Number(n.value)) }); dialog.close(); toast('Window created.'); await load(); });
            }
            if (action === 'window-open' || action === 'window-close') {
                const r = await post('/admin/windows/' + id + '/' + action.slice(7));
                toast(r.message);
                await load();
            }
            if (action === 'window-delete') {
                const row = windows.find(w => w.id === Number(id));
                const dialog = modal('Delete advising window', '<p>Delete <strong>' + esc(row.name) + "</strong> from the advising schedule? Pending holds will be released. Confirmed courses will remain.</p><form id=\"delete-window-form\"><div class=\"actions layout-7\"><button type=\"submit\" class=\"btn danger\">Delete window</button></div></form>");
                form('#delete-window-form', async () => { const r = await api('/admin/windows/' + id, { method: 'DELETE' }); dialog.close(); toast(r.message); await load(); });
            }
        });
        return;
    }
}
