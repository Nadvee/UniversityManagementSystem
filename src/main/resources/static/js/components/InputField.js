import { Component } from '../core/Component.js';
export class InputField extends Component {
    render(label, name, type = 'text', value = '', required = true, id = name) { const { esc } = this.dependencies; return '<div class="field"><label for="' + esc(id) + '">' + esc(label) + '</label><input id="' + esc(id) + '" name="' + esc(name) + '" type="' + type + '" value="' + esc(value) + '" ' + (required ? 'required' : '') + '></div>'; }
}
