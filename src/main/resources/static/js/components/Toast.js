import { Component } from '../core/Component.js';
export class Toast extends Component {
    render(message, error = false) { const { $, $$, esc, formData, toast } = this.dependencies; $('.toast')?.remove(); const node = document.createElement('div'); node.className = 'toast' + (error ? ' error' : ''); node.role = 'status'; node.textContent = message; document.body.append(node); setTimeout(() => node.remove(), 5500); }
}
