import { Component } from '../core/Component.js';
export class ActionButton extends Component {
    render(label, action, id = '', style = '') { const { $, $$, esc, formData, toast } = this.dependencies; return '<button type="button" class="btn small ' + style + '" data-action="' + esc(action) + '" data-id="' + esc(id) + '">' + esc(label) + '</button>'; }
}
