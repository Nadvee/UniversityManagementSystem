import { StudentPage } from '../../core/StudentPage.js';
import { WindowBanner } from '../../components/WindowBanner.js';
import { CountdownTimer } from '../../components/CountdownTimer.js';
export class StudentDashboardPage extends StudentPage {
    constructor() { super('dashboard'); }
    async render() {
        const ctx = this.context;
        const { page, content, ref, user } = ctx;
        const { $, esc, api, panel, body, metric, badge, fmtDate, routine } = this.ui.bindings(this.api);
        const windowBanner = new WindowBanner(this.ui).render.bind(new WindowBanner(this.ui));
        const timers = new CountdownTimer(this.ui).render.bind(new CountdownTimer(this.ui));
        $('#subtitle').textContent = 'A clear view of your academic day.';
        const load = this.watch(() => Promise.all([api('/dashboard/me'), api('/notices')]), ([data, notices]) => {
        const welcome = '<div class="welcome welcome-student"><div><h2>Welcome back, ' + esc(user.full_name.split(' ')[0]) + '.</h2><p>' + ({ STUDENT: 'Plan your courses. Stay on track. Make this semester yours.', TEACHER: 'Your teaching, course requests and student progress, in one place.', ADMIN: 'Manage your academic community and keep advising running smoothly.' }[user.role]) + '</p></div><span class="term">' + esc(ctx.ref.semesters.find(s => s.is_current)?.name || 'Current semester') + '</span></div>';
        const noticeHTML = body(notices.map(n => '<article class="notice">' + badge(n.category) + "<h3 class=\"layout-3\">" + esc(n.title) + '</h3><p>' + esc(n.body) + '</p>' + (n.start_at ? '<p>' + fmtDate(n.start_at) + ' — ' + fmtDate(n.end_at) + '</p>' : '') + '<small>' + fmtDate(n.published_at) + '</small></article>').join('') || '<p class="muted">No notices at the moment.</p>');
        const p = data.profile;
        const links = [['advising', 'Advising', '▤'], ['routine', 'Class Routine', '▦'], ['drop', 'Semester Drop', '⊖']];
        content.innerHTML = welcome + windowBanner(data.windows) + '<div class="metrics">' + metric('Current CGPA', p.cgpa == null ? 'N/A' : Number(p.cgpa).toFixed(2), 'Academic standing: ' + p.status) + metric('Credits completed', p.total_credits_completed, 'Academic record', 'green') + metric('Weekly classes', data.routine.length, 'Your confirmed course sessions') + metric('Selected credits', data.selectedCredit ?? 0, 'Confirmed courses and active holds', 'gold') + '</div><div class="grid"><div class="span-8">' + panel('Quick access', body('<div class="quick-links">' + links.map(l => '<a class="quick-link" href="/student/' + l[0] + '.html"><span>' + l[2] + '</span>' + l[1] + '</a>').join('') + '</div>')) + panel('This week’s classes', routine(data.routine), 'Saturday – Thursday', '<a href="/student/routine.html" class="muted">View routine →</a>') + '</div><div class="span-4">' + panel('Student profile', body([['Name', user.full_name], ['Student ID', user.login_id], ['Department', p.department], ['Semester', p.semester]].map(([k, v]) => '<div class="profile-line"><span>' + k + '</span><strong>' + esc(v) + '</strong></div>').join(''))) + panel('Notice board', noticeHTML) + '</div></div>';
        timers();
        });
        await load();
        this.every(timers, 1000);
    }
}
