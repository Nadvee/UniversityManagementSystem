import { Component } from '../core/Component.js';
export class WindowBanner extends Component {
    render(windows) {
        const { esc, fmtDate } = this.dependencies;
        const now = Date.now();
        const open = windows.find(w => w.status === 'OPEN' && new Date(w.start_at) <= now && new Date(w.end_at) > now);
        if (open)
            return '<div class="alert">● Advising is open: <span class="timer" data-until="' + esc(open.end_at) + '"></span> remaining <a href="/student/advising.html">Continue to Advising →</a></div>';
        const next = windows.filter(w => w.status === 'SCHEDULED' && new Date(w.start_at) > now)
            .reduce((nearest, window) => !nearest || new Date(window.start_at) < new Date(nearest.start_at) ? window : nearest, null);
        return '<div class="alert warning">Advising is not open.' + (next ? ' Next window: ' + fmtDate(next.start_at) : ' Watch the notice board for the next session.') + '</div>';
    }
}
