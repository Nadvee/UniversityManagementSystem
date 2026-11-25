export class Session {
    token() { return localStorage.getItem('mirpur-token') || ''; }
    save(token) { localStorage.setItem('mirpur-token', token); }
    logout() { localStorage.removeItem('mirpur-token'); }
    isLoggedIn() { return !!this.token(); }
    homePath(role) { return '/' + role.toLowerCase() + '/dashboard.html'; }
}
