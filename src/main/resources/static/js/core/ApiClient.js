export class ApiClient {
    #session;
    constructor(session) { this.#session = session; }
    async request(path, options = {}) {
        const response = await fetch('/api' + path, { cache: 'no-store', ...options, headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + (this.#session.token() || ''), ...options.headers }, body: options.body === undefined ? undefined : JSON.stringify(options.body) });
        let body;
        try {
            body = await response.json();
        }
        catch {
            body = {};
        }
        if (response.status === 401 && path != '/auth/login') {
            this.#session.logout();
            location.replace('/login.html');
        }
        if (!response.ok) {
            const error = new Error(body.message || 'The request could not be completed.');
            error.status = response.status;
            throw error;
        }
        return body;
    }
    post(path, body) { return this.request(path, { method: 'POST', body }); }
}
