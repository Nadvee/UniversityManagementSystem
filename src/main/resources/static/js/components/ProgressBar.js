import { Component } from '../core/Component.js';
export class ProgressBar extends Component {
    update(node, percentage) { node.style.width = percentage + '%'; }
}
