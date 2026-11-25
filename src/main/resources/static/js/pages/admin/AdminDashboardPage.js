import { AdminPage } from '../../core/AdminPage.js';
import { CountdownTimer } from '../../components/CountdownTimer.js';
export class AdminDashboardPage extends AdminPage {
    constructor() { super('dashboard'); }
    async render() {
        const ctx = this.context;
        const { page, content, ref, user } = ctx;
        const { $, esc, api, table, panel, body, metric, badge, fmtDate } = this.ui.bindings(this.api);
        const timers = new CountdownTimer(this.ui).render.bind(new CountdownTimer(this.ui));
        const metricsMarkup = state => metric('Students', state.students, 'Admin-created student accounts') + metric('Teachers', state.teachers, 'Faculty community') + metric('Sections', state.sections, 'Current academic catalogue') + metric('Open windows', state.windows.filter(w => w.status === 'OPEN').length, 'Advising control', 'green');
        const activityMarkup = state => table(state.audit, [['ID', 'id'], ['Action', r => badge(r.action)], ['By', 'actor'], ['Details', 'detail'], ['Time', r => fmtDate(r.created_at)]]);
        $('#subtitle').textContent = 'A clear view of your academic day.';
        const load = this.watch(() => Promise.all([api('/dashboard/me'), api('/notices')]), ([data, notices]) => {
        const welcome = '<div class="welcome welcome-admin"><div><h2>Welcome back, ' + esc(user.full_name.split(' ')[0]) + '.</h2><p>' + ({ STUDENT: 'Plan your courses. Stay on track. Make this semester yours.', TEACHER: 'Your teaching, course requests and student progress, in one place.', ADMIN: 'Manage your academic community and keep advising running smoothly.' }[user.role]) + '</p></div><span class="term">' + esc(ctx.ref.semesters.find(s => s.is_current)?.name || 'Current semester') + '</span></div>';
        const noticeHTML = body(notices.map(n => '<article class="notice">' + badge(n.category) + "<h3 class=\"layout-3\">" + esc(n.title) + '</h3><p>' + esc(n.body) + '</p>' + (n.start_at ? '<p>' + fmtDate(n.start_at) + ' — ' + fmtDate(n.end_at) + '</p>' : '') + '<small>' + fmtDate(n.published_at) + '</small></article>').join('') || '<p class="muted">No notices at the moment.</p>');
        content.innerHTML = welcome + '<div class="metrics" id="dashboard-metrics">' + metricsMarkup(data) + '</div><div class="grid"><div class="span-8">' + panel('Recent activity', '<div id="recent-activity">' + activityMarkup(data) + '</div>', 'Latest 8 account, teaching and advising changes') + panel('Administration shortcuts', body('<div class="quick-links">' + [['students', 'Manage Students', '♧'], ['teachers', 'Manage Teachers', '♧'], ['advising-control', 'Advising Control', '◷'], ['teacher-requests', 'Teacher Requests', '☷'], ['offerings', 'Course Offerings', '▤']].map(l => '<a class="quick-link" href="' + l[0] + '.html"><span>' + l[2] + '</span>' + l[1] + '</a>').join('') + '</div>')) + '</div><div class="span-4">' + panel('Notice board', noticeHTML) + '</div></div>';
        timers();
        });
        await load();
        this.every(timers, 1000);
    }
}
