export class Component {
    #kit;
    constructor(kit) { this.#kit = kit; }
    get dependencies() { return this.#kit.bindings(); }
    get signal() { return this.#kit.signal; }
    runBusy(callback) { return this.#kit.runBusy(callback); }
    destroy() { }
}
