export class BaseApi {
    #client;
    constructor(client) { this.#client = client; }
    request(path, options) { return this.#client.request(path, options); }
    post(path, body) { return this.#client.post(path, body); }
    reference() { return this.request('/reference'); }
    currentUser() { return this.request('/auth/me'); }
}
