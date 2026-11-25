import { Component } from '../core/Component.js';
export class PanelBody extends Component {
    render(html) { const { $, $$, esc, formData, toast } = this.dependencies; return '<div class="panel-body">' + html + '</div>'; }
}
