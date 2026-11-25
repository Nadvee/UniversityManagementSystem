import { AdminPage } from '../../core/AdminPage.js';
export class TeacherRequestsPage extends AdminPage {
    constructor() { super('teacher-requests'); }
    async render() {
        const { content } = this.context;
        const { $, esc, api, post, toast, table, panel, body, badge, actions, button, select, form, modal } = this.ui.bindings(this.api);
        let choices = [], sections = [];
        const load = this.watch(() => Promise.all([api('/admin/teacher-choices'), api('/admin/offerings')]), data => { [choices, sections] = data; content.innerHTML = panel('Teacher course requests', body('<input class="search-input" id="request-filter" placeholder="Filter by teacher, course or status" aria-label="Filter teacher requests">') + '<div id="requests"></div>', 'Review confirmed teacher choices and assign teachers to sections.'); const render = () => { const q = $('#request-filter').value.toLowerCase(); $('#requests').innerHTML = table(choices.filter(r => (r.full_name + ' ' + r.login_id + ' ' + r.code + ' ' + r.name + ' ' + r.status).toLowerCase().includes(q)), [['Teacher', r => esc(r.full_name) + '<br><span class="muted">' + esc(r.login_id) + '</span>'], ['Code', 'code'], ['Course', 'name'], ['Credit', 'credit'], ['Other requests', 'competing'], ['Status', r => badge(r.status)], ['Source', 'source'], ['Actions', r => r.status === 'CONFIRMED' ? '<div class="actions">' + button('Assign', 'request-assign', r.id) + button('Reject', 'request-reject', r.id, 'danger') + '</div>' : '']]); }; $('#request-filter').addEventListener("input", render); render(); });
        await load();
        actions(async (action, id) => {
            if (action === 'request-reject') {
                await post('/admin/teacher-choices/' + id + '/reject');
                toast('Request rejected.');
                await load();
            }
            if (action === 'request-assign') {
                const choice = choices.find(c => c.id === Number(id));
                const available = sections.filter(s => s.course_id === choice.course_id).sort((a, b) => Number(!!a.teacher_id) - Number(!!b.teacher_id));
                const dialog = modal('Assign ' + choice.code, available.length ? '<form id="assign-form">' + select('Section', 'sectionId', available.map(s => [s.id, 'Section ' + s.section_no + ' · ' + s.schedule + ' · ' + s.room + (s.faculty ? ' · Currently: ' + s.faculty : ' · Unassigned')])) + "<p class=\"muted layout-11\">Assigning a section with a teacher replaces its current assignment.</p><button class=\"btn layout-7\">Assign teacher</button></form>" : '<p>No sections are available for this course.</p><a class="btn" href="offerings.html">Create a section</a>');
                form('#assign-form', async (data) => { await post('/admin/teacher-choices/' + id + '/assign', { sectionId: Number(data.sectionId) }); dialog.close(); toast('Teacher assigned.'); await load(); });
            }
        });
        return;
    }
}
