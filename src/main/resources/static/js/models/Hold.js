export class Hold {
    #expiresAt;
    constructor(expiresAt) { this.#expiresAt = new Date(expiresAt).getTime(); }
    remainingSeconds(now = Date.now()) { return Math.max(0, Math.floor((this.#expiresAt - now) / 1000)); }
    isExpired(now = Date.now()) { return this.remainingSeconds(now) === 0; }
    display(now = Date.now()) {
        const seconds = this.remainingSeconds(now);
        return [Math.floor(seconds / 3600), Math.floor(seconds / 60) % 60, seconds % 60].map(value => String(value).padStart(2, '0')).join(':');
    }
}
