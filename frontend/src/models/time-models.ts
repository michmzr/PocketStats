export class TimePeriod {
    start: Date | undefined
    end: Date | undefined

    constructor(start: Date | undefined, end: Date | undefined) {
        this.start = start;
        this.end = end;
        this.validate();
    }

    validate() {
        if (this.start && this.end && this.start > this.end) {
            const tmp = this.end
            this.end = this.start
            this.start = tmp
        }
    }
}