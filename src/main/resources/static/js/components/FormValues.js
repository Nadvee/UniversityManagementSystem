import { Component } from '../core/Component.js';
export class FormValues extends Component {
    render(form) { const { $, $$, esc, formData, toast } = this.dependencies; return Object.fromEntries(new FormData(form)); }
}
