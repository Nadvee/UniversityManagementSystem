export class PasswordSetupForm {
    #api;
    #onComplete;
    #form;
    constructor(api, onComplete) {
        this.#api = api;
        this.#onComplete = onComplete;
        this.#form = document.querySelector('#password-setup-form');
    }
    open(id) {
        this.#form.reset();
        document.querySelector('#setup-error').textContent = '';
        document.querySelector('#setup-id').value = id;
        this.#form.hidden = false;
        document.querySelector('#setup-id').focus();
    }
    close() {
        this.#form.hidden = true;
        this.#form.reset();
    }
    bind() {
        this.#form.addEventListener('submit', async event => {
            event.preventDefault();
            const error = document.querySelector('#setup-error');
            const button = this.#form.querySelector('button[type=submit]');
            const back = document.querySelector('#back-to-login');
            error.textContent = '';
            button.disabled = true;
            back.disabled = true;
            try {
                const id = document.querySelector('#setup-id').value.trim();
                const password = document.querySelector('#setup-password').value;
                const confirmPassword = document.querySelector('#setup-confirm').value;
                if (password !== confirmPassword) throw new Error('Passwords do not match.');
                const result = await this.#api.post('/auth/setup-password', { id, password, confirmPassword });
                this.close();
                this.#onComplete(id, result.message);
            } catch (problem) {
                error.textContent = problem.message;
            } finally {
                button.disabled = false;
                back.disabled = false;
            }
        });
    }
}
