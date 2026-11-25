import { Component } from '../core/Component.js';
export class DataTable extends Component {
    render(rows, columns, empty = 'No records to display.') {
        const { $, $$, esc, formData, toast } = this.dependencies;
        if (!rows.length)
            return '<div class="empty"><strong>' + esc(empty) + '</strong><span>Records will appear here when available.</span></div>';
        return '<div class="table-wrap"><table><thead><tr>' + columns.map(c => '<th>' + esc(c[0]) + '</th>').join('') + '</tr></thead><tbody>' + rows.map(row => '<tr>' + columns.map(c => '<td>' + (typeof c[1] === 'function' ? c[1](row) : esc(row[c[1]] ?? '—')) + '</td>').join('') + '</tr>').join('') + '</tbody></table></div>';
    }
}
