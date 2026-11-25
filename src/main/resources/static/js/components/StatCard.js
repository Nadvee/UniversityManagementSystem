import { Component } from '../core/Component.js';
export class StatCard extends Component {
    render(label, value, foot = '', tone = '') { const { $, $$, esc, formData, toast } = this.dependencies; return '<article class="metric ' + tone + '"><div class="metric-label">' + esc(label) + '</div><strong>' + esc(value) + '</strong><small>' + esc(foot) + '</small></article>'; }
}
