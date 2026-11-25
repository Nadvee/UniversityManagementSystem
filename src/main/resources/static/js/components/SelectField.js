import { Component } from '../core/Component.js';
export class SelectField extends Component {
    render(label, name, options, value = '', id = name) { const { esc } = this.dependencies; return '<div class="field"><label for="' + esc(id) + '">' + esc(label) + '</label><select id="' + esc(id) + '" name="' + esc(name) + '">' + options.map(o => '<option value="' + esc(o[0]) + '" ' + (String(o[0]) === String(value) ? 'selected' : '') + '>' + esc(o[1]) + '</option>').join('') + '</select></div>'; }
}
