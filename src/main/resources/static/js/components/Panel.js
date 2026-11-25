import { Component } from '../core/Component.js';
export class Panel extends Component {
    render(title, body, subtitle = '', actions = '') { const { $, $$, esc, formData, toast } = this.dependencies; return '<section class="panel"><div class="panel-head"><div><h2>' + esc(title) + '</h2>' + (subtitle ? '<p>' + esc(subtitle) + '</p>' : '') + '</div>' + actions + '</div>' + body + '</section>'; }
}
