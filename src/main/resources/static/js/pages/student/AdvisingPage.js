import {ProgressBar} from '../../components/ProgressBar.js';
import { StudentPage } from '../../core/StudentPage.js';
import { WindowBanner } from '../../components/WindowBanner.js';
import { CountdownTimer } from '../../components/CountdownTimer.js';
export class AdvisingPage extends StudentPage {
    constructor() { super('advising'); }
    async render() {
        const { content } = this.context;
        const { $, esc, api, post, toast, table, panel, body, actions, button, courseColumns } = this.ui.bindings(this.api);
        const windowBanner = new WindowBanner(this.ui).render.bind(new WindowBanner(this.ui));
        const timers = new CountdownTimer(this.ui).render.bind(new CountdownTimer(this.ui));
        $('#subtitle').textContent = 'Hold your seat, review your choices and confirm within ten minutes.';
        content.innerHTML = '<div id="window-banner"></div><div id="credit-summary"></div>' + panel('Selected courses · on hold', '<div id="holds"></div>', 'Your seat is reserved until the server hold expires.') + panel('Final confirmed courses', '<div id="confirmed"></div>') + panel('Courses open for advising', '<div id="open-courses"></div>');
        const refresh = this.watch(() => Promise.all([api('/advising/state'), api('/advising/courses')]), ([s, c]) => {
                $('#window-banner').innerHTML = windowBanner(s.windows);
                $('#credit-summary').innerHTML=body('<div class="actions layout-13"><strong>Selected credit: '+s.credit+' / '+s.band.max_credit+'</strong><span class="muted">'+esc(s.band.status_label)+' · Minimum '+s.band.min_credit+'</span></div><div class="progress layout-14"><span data-progress></span></div>');new ProgressBar(this.ui).update($('#credit-summary [data-progress]'),Math.min(100,100*s.credit/s.band.max_credit));
                $('#holds').innerHTML = table(s.holds, [...courseColumns, ['Expires in', r => '<span class="timer" data-until="' + esc(r.expires_at) + '"></span>'], ['Actions', r => '<div class="actions">' + button('Confirm', 'hold-confirm', r.hold_id) + button('Remove', 'hold-remove', r.hold_id, 'light') + '</div>']], 'No courses on hold.');
                $('#confirmed').innerHTML = table(s.confirmed, courseColumns) + body('<strong>Confirmed credit: ' + s.confirmed.reduce((n, r) => n + Number(r.credit), 0) + '</strong>');
                const columns = [...courseColumns, ['Seats', r => esc((r.available ?? 0) + ' / ' + r.capacity)], ['', r => s.window && c.some(x => x.id === r.id) ? button('Select', 'hold-select', r.id) : '<span class="muted">Not open</span>']];
                $('#open-courses').innerHTML = table(c, columns, 'No courses are open for advising.');
                timers();
        });
        await refresh();
        this.every(timers, 1000);
        actions(async (action, id) => {
            if (!action.startsWith('hold-'))
                return;
            if (action === 'hold-select')
                await post('/advising/select', { sectionId: Number(id) });
            if (action === 'hold-confirm')
                await post('/advising/confirm', { holdId: Number(id) });
            if (action === 'hold-remove')
                await api('/advising/hold/' + id, { method: 'DELETE' });
            toast(action === 'hold-confirm' ? 'Course confirmed.' : action === 'hold-select' ? 'Seat held for ten minutes.' : 'Hold removed.');
            await refresh();
        });
        return;
    }
}
