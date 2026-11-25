import { AdminPage } from '../../core/AdminPage.js';
export class SectionEditorPage extends AdminPage {
    constructor(page) { super(page); }
    async render() {
        const { page, content, ref } = this.context;
        const { $, $$, esc, api, toast, table, panel, body, badge, actions, button, input, select, form, courseColumns, modal } = this.ui.bindings(this.api);
        let rows = [], filter = '';
        const offeringStatus = r => {
            const status = r.is_offered ? 'Enabled' : 'Disabled';
            if (page !== 'offerings') return badge(status);
            const label = (r.is_offered ? 'Disable' : 'Enable') + ' offering for ' + r.code + ' section ' + r.section_no;
            return '<button type="button" class="badge warn offering-status" data-action="section-status" data-id="' + esc(r.id) + '" aria-pressed="' + r.is_offered + '" aria-label="' + esc(label) + '" title="' + esc(label) + '">' + status + '</button>';
        };
        const load = this.watch(() => api('/admin/offerings'), data => {
            rows = data;
            content.innerHTML = panel(page === 'routine' ? 'Section schedules' : 'Course offerings', body('<input class="search-input" id="section-filter" value="' + esc(filter) + '" placeholder="Search course, faculty, section or room" aria-label="Search sections">') + '<div id="sections"></div>', 'Teachers are assigned through Teacher Requests.', page === 'offerings' ? button('Add section', 'section-add', '', 'gold') : button('Print / Save PDF', 'print'));
            const render = () => {
                const q = filter.toLowerCase();
                $('#sections').innerHTML = table(rows.filter(s => (s.code + ' ' + s.name + ' ' + s.faculty + ' ' + s.room + ' ' + s.section_no).toLowerCase().includes(q)), [...courseColumns, ['Section', 'section_no'], ['Seats', r => esc(r.seats_taken + ' / ' + r.capacity)], ['Offered', offeringStatus], ['', r => '<div class="actions">' + button('Edit', 'section-edit', r.id, 'light') + button('Enrolled Students', 'section-roster', r.id, 'light') + (page === 'offerings' ? button('Delete', 'section-delete', r.id, 'danger') : '') + '</div>']]);
            };
            $('#section-filter').addEventListener('input', () => { filter = $('#section-filter').value; render(); });
            render();
        });
        await load();
        const editor = async (id) => {
            const s = rows.find(r => r.id === Number(id));
            const slots = id ? await api('/admin/offerings/' + id + '/schedules') : [{ day: 'SAT', start: '09:00', end: '10:30' }];
            const dialog = modal(id ? 'Edit section' : 'Add section', '<form id="section-form"><div class="form-grid two">' + select('Course', 'courseId', ref.courses.filter(c => !c.code.startsWith('DEMO')).map(c => [c.id, c.code + ' · ' + c.name]), s?.course_id) + input('Section number', 'sectionNo', 'text', s?.section_no || '') + select('Room', 'roomId', ref.rooms.map(r => [r.id, r.name + ' (capacity ' + r.capacity + ')']), s?.room_id) + input('Capacity', 'capacity', 'number', s?.capacity || 40) + select('Offering status', 'offered', [['true', 'Enabled'], ['false', 'Disabled']], String(s?.is_offered ?? true)) + "</div><h3 class=\"layout-9\">Schedule slots</h3><div id=\"schedule-slots\"></div><button class=\"btn light small\" type=\"button\" id=\"add-slot\">Add time slot</button><div class=\"actions layout-10\"><button class=\"btn\" type=\"submit\">Save section</button></div></form>");
            let slotNumber = 0;
            const addSlot = s => { const prefix = 'schedule-slot-' + slotNumber++; const div = document.createElement('div'); div.className = 'form-grid schedule-slot';  div.innerHTML = select('Day', 'day', [['SAT', 'Saturday'], ['SUN', 'Sunday'], ['MON', 'Monday'], ['TUE', 'Tuesday'], ['WED', 'Wednesday'], ['THU', 'Thursday']], s.day, prefix + '-day') + input('Start', 'start', 'time', s.start.slice(0, 5), true, prefix + '-start') + input('End', 'end', 'time', s.end.slice(0, 5), true, prefix + '-end') + '<button type="button" class="btn danger small">Remove</button>'; $('button', div).addEventListener("click", () => div.remove()); $('#schedule-slots').append(div); };
            slots.forEach(addSlot);
            $('#add-slot').addEventListener("click", () => addSlot({ day: 'SUN', start: '09:00', end: '10:30' }));
            form('#section-form', async (data) => { const schedules = $$('.schedule-slot').map(n => ({ day: $('[name=day]', n).value, start: $('[name=start]', n).value, end: $('[name=end]', n).value })); const payload = { courseId: Number(data.courseId), sectionNo: data.sectionNo, roomId: Number(data.roomId), capacity: Number(data.capacity), offered: data.offered === 'true', schedules }; await api('/admin/offerings' + (id ? '/' + id : ''), { method: id ? 'PUT' : 'POST', body: payload }); dialog.close(); toast('Section saved.'); await load(); });
        };
        actions(async (action, id) => {
            if (action === 'section-delete' && page === 'offerings') {
                const row = rows.find(r => r.id === Number(id));
                const dialog = modal('Delete course offering', '<p>Delete <strong>' + esc(row.code) + ' · ' + esc(row.name) + ' · Section ' + esc(row.section_no) + '</strong>?</p><p>This section will be removed from offerings and routines. Its enrollments will be cancelled and held seats released.</p><p>This cannot be undone.</p><form id="delete-section-form"><div class="actions layout-7"><button type="button" class="btn light" id="cancel-section-delete">Cancel</button><button type="submit" class="btn danger">Delete offering</button></div></form>');
                $('#cancel-section-delete', dialog).addEventListener('click', () => dialog.close());
                form('#delete-section-form', async () => {
                    const result = await api('/admin/offerings/' + id, { method: 'DELETE' });
                    dialog.close();
                    toast(result.message);
                    await load();
                });
            }
            if (action === 'section-status' && page === 'offerings') {
                const row = rows.find(r => r.id === Number(id));
                const result = await api('/admin/offerings/' + id + '/status', { method: 'PATCH', body: { offered: !row.is_offered } });
                toast(result.message);
                await load();
            }
            if (action === 'section-add')
                await editor();
            if (action === 'section-edit')
                await editor(id);
            if (action === 'section-roster') {
                const path = '/admin/sections/' + id + '/students';
                const columns = [['Student ID', 'login_id'], ['Name', 'full_name']];
                const dialog = modal('Enrolled Students', table(await api(path), columns));
                this.watchDialog(dialog, () => api(path), data => { $('.dialog-body', dialog).innerHTML = table(data, columns); });
            }
        });
        return;
    }
}
