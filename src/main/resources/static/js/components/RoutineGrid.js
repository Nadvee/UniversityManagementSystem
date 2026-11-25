import { Component } from '../core/Component.js';
export class RoutineGrid extends Component {
    render(rows, emptyTitle = 'Your routine is ready for a fresh start.', emptyHint = 'Confirm courses in Advising to see your weekly classes.') {
        const { $, $$, esc, formData, toast } = this.dependencies;
        const days = ['SAT', 'SUN', 'MON', 'TUE', 'WED', 'THU'];
        const times = [...new Set(rows.map(r => r.start_time))].sort();
        if (!rows.length)
            return '<div class="empty"><strong>' + esc(emptyTitle) + '</strong><span>' + esc(emptyHint) + '</span></div>';
        return '<div class="table-wrap"><table class="routine"><thead><tr><th>Time</th>' + days.map(d => '<th>' + d + '</th>').join('') + '</tr></thead><tbody>' + times.map(t => '<tr><th>' + esc(t.slice(0, 5)) + '</th>' + days.map(d => '<td>' + rows.filter(r => r.day === d && r.start_time === t).map(r => '<div class="class-cell"><strong>' + esc(r.code) + '</strong><small>' + esc(r.room) + ' · ' + esc(r.faculty) + '</small><small>' + esc(r.start_time.slice(0, 5)) + ' – ' + esc(r.end_time.slice(0, 5)) + '</small></div>').join('') + '</td>').join('') + '</tr>').join('') + '</tbody></table></div>';
    }
}
