import { PublicPage } from '../core/PublicPage.js';
import { PasswordSetupForm } from '../components/PasswordSetupForm.js';
export class LoginPage extends PublicPage {
    #passwordSetup;
    constructor() { super('login'); }
    bind() {
        const { $, api } = this.ui.bindings(this.api);
        const showLogin = (id, notice = '') => {
            $('#login-form').hidden = false;
            $('#login-id').value = id;
            $('#login-password').value = '';
            $('#login-error').textContent = '';
            $('#login-notice').textContent = notice;
            $('#login-password').focus();
        };
        this.#passwordSetup = new PasswordSetupForm(this.api, showLogin);
        this.#passwordSetup.bind();
        $('#show-password-setup').addEventListener('click', () => {
            const id = $('#login-id').value.trim();
            $('#login-form').hidden = true;
            $('#login-password').value = '';
            this.#passwordSetup.open(id);
        });
        $('#back-to-login').addEventListener('click', () => {
            const id = $('#setup-id').value.trim();
            this.#passwordSetup.close();
            showLogin(id);
        });
        $('#login-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            $('#login-error').textContent = '';
            const button = $('button', e.target);
            button.disabled = true;
            try {
                const result = await api('/auth/login', { method: 'POST', body: { id: $('#login-id').value.trim(), password: $('#login-password').value } });
                this.session.save(result.token);
                location.replace(this.session.homePath(result.role));
            }
            catch {
                $('#login-error').textContent = 'Invalid ID or password.';
            }
            finally {
                button.disabled = false;
            }
        });
    }
}
