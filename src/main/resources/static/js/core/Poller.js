export class Poller {
    #timer;
    start(callback, interval) { this.stop(); this.#timer = setInterval(callback, interval); return this; }
    stop() {
        if (this.#timer)
            clearInterval(this.#timer);
        this.#timer = null;
    }
}
