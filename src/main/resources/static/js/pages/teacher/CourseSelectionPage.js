import { TeacherPage } from '../../core/TeacherPage.js';
export class CourseSelectionPage extends TeacherPage {
    constructor() { super('course-selection'); }
    async render() {
        const { content } = this.context;
        const { $, $$, api, post, toast, table, panel, body, badge, actions, button } = this.ui.bindings(this.api);
        $('#subtitle').textContent = 'Select up to four courses. Confirm each selected course for administrator review.';
        let rows = [], filter = '', busy = false;
        const selected = row => ['SELECTED', 'CONFIRMED', 'ASSIGNED'].includes(row.status);
        const columns = [['Code', 'code'], ['Course', 'name'], ['Credit', 'credit'], ['Department', 'department'], ['Status', r => badge(r.status) + (r.source === 'ADMIN' ? '<br><span class="muted">Set by admin</span>' : '')]];
        const disableActions = () => $$('[data-action^="choice-"]').forEach(node => node.disabled = busy);
        const selectedActions = row => {
            const confirm = row.status === 'SELECTED' ? button('Confirm', 'choice-confirm', row.id, 'gold') : '<button type="button" class="btn small light" disabled>Confirmed</button>';
            const remove = row.status === 'ASSIGNED' ? '<button type="button" class="btn small light" disabled title="Ask the administrator to unassign this course first.">Remove</button>' : button('Remove', 'choice-remove', row.id, 'light');
            return '<div class="actions">' + confirm + remove + '</div>';
        };
        const render = () => {
            $('#selected-courses').innerHTML = table(rows.filter(selected), [...columns, ['Section', 'assigned_section_id'], ['Actions', selectedActions]], 'No selected courses. Select a course from the list below.');
            const available = rows.filter(row => !selected(row) && (row.code + ' ' + row.name + ' ' + row.department).toLowerCase().includes(filter.toLowerCase()));
            $('#available-courses').innerHTML = table(available, [...columns, ['Actions', row => button('Select', 'choice-select', row.id)]], 'No available courses match this search.');
            disableActions();
        };
        const load = this.watch(() => api('/teacher/courses'), data => { rows = data; render(); });
        content.innerHTML = panel('Selected courses', '<div id="selected-courses"></div>', 'Confirm each course separately, or remove it to return it to the available list.') + panel('Courses offered this semester', body('<input class="search-input" id="filter" placeholder="Search department, course code or name" aria-label="Search courses">') + '<div id="available-courses"></div>');
        $('#filter').addEventListener('input', () => { filter = $('#filter').value; render(); });
        await load();
        actions(async (action, id) => {
            if (!['choice-select', 'choice-confirm', 'choice-remove'].includes(action) || busy) return;
            busy = true; disableActions();
            try {
                const path = '/teacher/choices/' + id;
                const result = action === 'choice-select' ? await post(path) : action === 'choice-confirm' ? await post(path + '/confirm') : await api(path, { method: 'DELETE' });
                toast(result.message);
                await load();
            } finally { busy = false; disableActions(); }
        });
    }
}
