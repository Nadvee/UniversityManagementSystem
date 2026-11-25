import { TeacherPage } from '../../core/TeacherPage.js';
import { CountdownTimer } from '../../components/CountdownTimer.js';
export class TeacherDashboardPage extends TeacherPage {
    constructor() { super('dashboard'); }
    async render() {
        const ctx = this.context;
        const { page, content, ref, user } = ctx;
        const { $, esc, api, panel, body, metric, badge, fmtDate } = this.ui.bindings(this.api);
        const timers = new CountdownTimer(this.ui).render.bind(new CountdownTimer(this.ui));
        $('#subtitle').textContent = 'A clear view of your academic day.';
        const load = this.watch(() => Promise.all([api('/dashboard/me'), api('/notices')]), ([data, notices]) => {
        const welcome = '<div class="welcome welcome-teacher"><div><h2>Welcome back, ' + esc(user.full_name.split(' ')[0]) + '.</h2><p>' + ({ STUDENT: 'Plan your courses. Stay on track. Make this semester yours.', TEACHER: 'Your teaching, course requests and student progress, in one place.', ADMIN: 'Manage your academic community and keep advising running smoothly.' }[user.role]) + '</p></div><span class="term">' + esc(ctx.ref.semesters.find(s => s.is_current)?.name || 'Current semester') + '</span></div>';
        const noticeHTML = body(notices.map(n => '<article class="notice">' + badge(n.category) + "<h3 class=\"layout-3\">" + esc(n.title) + '</h3><p>' + esc(n.body) + '</p>' + (n.start_at ? '<p>' + fmtDate(n.start_at) + ' — ' + fmtDate(n.end_at) + '</p>' : '') + '<small>' + fmtDate(n.published_at) + '</small></article>').join('') || '<p class="muted">No notices at the moment.</p>');
        content.innerHTML = welcome + '<div class="metrics">' + metric('Assigned sections', data.assigned, 'Current semester') + metric('Pending choices', data.pending, 'Draft and confirmed requests', 'gold') + metric('Teacher ID', user.login_id, 'Your faculty account') + metric('Teaching workspace', 'Ready', 'Choose courses and view assigned sections', 'green') + '</div><div class="grid"><div class="span-8">' + panel('Your teaching workspace', body('<div class="quick-links"><a class="quick-link" href="course-selection.html"><span>▤</span>Course Selection</a><a class="quick-link" href="my-courses.html"><span>▦</span>My Courses</a></div>')) + '</div><div class="span-4">' + panel('Notice board', noticeHTML) + '</div></div>';
        timers();
        });
        await load();
        this.every(timers, 1000);
    }
}
