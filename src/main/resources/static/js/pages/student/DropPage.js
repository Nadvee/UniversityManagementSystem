import { StudentPage } from '../../core/StudentPage.js';
export class DropPage extends StudentPage {
    constructor() { super('drop'); }
    async render() {
        const { content } = this.context;
        const { $, $$, esc, api, post, toast, table, panel, body, button, form, courseColumns, modal } = this.ui.bindings(this.api);
        const load = this.watch(() => api('/drop/eligible'), d => {
            const closed = new Date(d.rules.drop_deadline + 'T23:59:59') < new Date();
            content.innerHTML = '<div class="alert warning">At least ' + esc(d.band.min_credit) + ' credits must remain. Drop deadline: ' + esc(d.rules.drop_deadline) + '.</div>' + panel('Select courses to drop', '<form id="drop-form">' + table(d.courses, [['Select', r => '<input type="checkbox" name="section" value="' + r.id + '" aria-label="Drop ' + esc(r.code) + '">'], ...courseColumns]) + body('<div class="field"><label for="reason">Reason for dropping</label><textarea name="reason" id="reason" maxlength="1000" required></textarea></div><button class="btn danger" type="submit" ' + (closed ? 'disabled' : '') + '>Confirm drop</button>') + '</form>');
            form('#drop-form', async (d, f) => {
                const ids = $$('input[name=section]:checked', f).map(n => Number(n.value));
                if (!ids.length)
                    throw new Error('Select at least one course.');
                const dialog = modal('Confirm course drop', '<p>Drop ' + ids.length + ' selected course(s)? This updates your enrollment and class routine.</p><div class="actions">' + button('Confirm drop', 'drop-approved', '', 'danger') + '</div>');
                $('[data-action=drop-approved]', dialog).addEventListener("click", async (e) => {
                    e.target.disabled = true;
                    try {
                        const result = await post('/drop/confirm', { sectionIds: ids, reason: d.reason });
                        dialog.close();
                        toast(result.message);
                        await load();
                    }
                    catch (err) {
                        toast(err.message, true);
                        e.target.disabled = false;
                    }
                });
            });
        });
        await load();
        return;
    }
}
