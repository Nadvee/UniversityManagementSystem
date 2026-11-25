import { TeacherPage } from '../../core/TeacherPage.js';
export class MyCoursesPage extends TeacherPage {
    constructor() { super('my-courses'); }
    async render() {
        const { content } = this.context;
        const { $, esc, api, table, panel, badge, routine, actions, button, courseColumns, modal } = this.ui.bindings(this.api);
        const load = this.watch(() => api('/teacher/sections'), sections => {
        content.innerHTML = panel('Assigned sections', table(sections, [...courseColumns, ['Section', 'section_no'], ['Enrolled', r => esc(r.seats_taken + ' / ' + r.capacity)], ['Advising', r => badge(r.in_advising ? 'In advising' : 'Assigned, not yet in advising')], ['', r => button('Enrolled Students', 'roster', r.id, 'light')]]));
        const schedule = sections.flatMap(s => (s.schedule || '').split('; ').filter(Boolean).map(x => { const [day, time] = x.split(' '), [start_time, end_time] = time.split('-'); return { ...s, day, start_time, end_time }; }));
        content.innerHTML += panel('Teaching schedule', routine(schedule, 'No teaching schedule yet.', 'Assigned sections and their class times will appear here.'));
        });
        await load();
        actions(async (action, id) => {
            if (action === 'roster') {
                const path = '/teacher/sections/' + id + '/students';
                const columns = [['Student ID', 'login_id'], ['Name', 'full_name']];
                const dialog = modal('Enrolled Students', table(await api(path), columns));
                this.watchDialog(dialog, () => api(path), rows => { $('.dialog-body', dialog).innerHTML = table(rows, columns); });
            }
        });
        return;
    }
}
