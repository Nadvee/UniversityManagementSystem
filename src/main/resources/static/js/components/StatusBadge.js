import { Component } from '../core/Component.js';
export class StatusBadge extends Component {
    render(value) { const { esc } = this.dependencies; const s = String(value ?? 'Not selected'); return '<span class="badge ' + (/^(OPEN|CONFIRMED|ASSIGNED|NORMAL|ACTIVE|true)$/i.test(s) ? 'good' : /^(CLOSED|REJECTED|EXPIRED|PROBATION|INACTIVE|false)$/i.test(s) ? 'bad' : 'warn') + '">' + esc(s.replaceAll('_', ' ')) + '</span>'; }
}
