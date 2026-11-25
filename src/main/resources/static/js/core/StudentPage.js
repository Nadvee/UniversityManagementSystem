import { AuthenticatedPage } from './AuthenticatedPage.js';
export class StudentPage extends AuthenticatedPage {
    get requiredRole() { return 'STUDENT'; }
}
