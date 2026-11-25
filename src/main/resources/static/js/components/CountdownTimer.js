import { Component } from '../core/Component.js';
import { Hold } from '../models/Hold.js';
export class CountdownTimer extends Component {
    render() { document.querySelectorAll('[data-until]').forEach(node => { node.textContent = new Hold(node.dataset.until).display(); }); }
}
