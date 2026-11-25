import { LoginPage } from './pages/LoginPage.js';
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage.js';
import { AdvisingControlPage } from './pages/admin/AdvisingControlPage.js';
import { OfferingsPage } from './pages/admin/OfferingsPage.js';
import { StudentsPage } from './pages/admin/StudentsPage.js';
import { TeacherRequestsPage } from './pages/admin/TeacherRequestsPage.js';
import { TeachersPage } from './pages/admin/TeachersPage.js';
import { AdvisingPage } from './pages/student/AdvisingPage.js';
import { DropPage } from './pages/student/DropPage.js';
import { RoutinePage } from './pages/student/RoutinePage.js';
import { StudentDashboardPage } from './pages/student/StudentDashboardPage.js';
import { CourseSelectionPage } from './pages/teacher/CourseSelectionPage.js';
import { MyCoursesPage } from './pages/teacher/MyCoursesPage.js';
import { TeacherDashboardPage } from './pages/teacher/TeacherDashboardPage.js';
export class Application {
    #pages = {
        login: LoginPage,
        'admin/dashboard': AdminDashboardPage,
        'admin/teachers': TeachersPage,
        'admin/students': StudentsPage,
        'admin/teacher-requests': TeacherRequestsPage,
        'admin/offerings': OfferingsPage,
        'admin/advising-control': AdvisingControlPage,
        'student/dashboard': StudentDashboardPage,
        'student/advising': AdvisingPage,
        'student/routine': RoutinePage,
        'student/drop': DropPage,
        'teacher/dashboard': TeacherDashboardPage,
        'teacher/course-selection': CourseSelectionPage,
        'teacher/my-courses': MyCoursesPage
    };
    start() { const page = document.body.dataset.page || 'login'; const area = location.pathname.split('/')[1]; const key = area.endsWith('.html') ? page : area + '/' + page; const Page = this.#pages[key]; if (!Page)
        throw new Error('Unknown page.'); return new Page().init(); }
}
new Application().start();
