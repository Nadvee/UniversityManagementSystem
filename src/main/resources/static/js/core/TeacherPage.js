import { AuthenticatedPage } from './AuthenticatedPage.js';
export class TeacherPage extends AuthenticatedPage {
    get requiredRole() { return 'TEACHER'; }
}
