import { BasePage } from './BasePage.js';
import { ApplicationShell } from '../components/ApplicationShell.js';
export class AuthenticatedPage extends BasePage {
    get requiredRole() { return null; }
    async guard() {
        if (!this.session.isLoggedIn()) {
            location.replace('/login.html');
            return false;
        }
        this.context.user = await this.api.currentUser();
        const role = this.context.user.role;
        if (this.requiredRole && role !== this.requiredRole) {
            location.replace(this.session.homePath(role));
            return false;
        }
        return true;
    }
    mountLayout() { new ApplicationShell(this.ui).render(this.context.user, this.context.page); this.context.content = document.querySelector('#content'); }
    async load() { this.context.ref = await this.api.reference(); }
    bind() {
        const { actions, $ } = this.ui.bindings();
        actions(async (action) => {
            if (action === 'logout') {
                this.session.logout();
                location.replace('/login.html');
            }
            if (action === 'menu')
                $('.sidebar').classList.toggle('open');
            if (action === 'print')
                window.print();
        });
    }
}
