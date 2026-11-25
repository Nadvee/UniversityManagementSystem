import { AuthenticatedPage } from './AuthenticatedPage.js';
export class AdminPage extends AuthenticatedPage {
    get requiredRole() { return 'ADMIN'; }
}
