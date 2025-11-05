class Semaphore {
    protected int value = 0;

    protected Semaphore() {
        this.value = 0;
    }

    protected Semaphore(int initial) {
        this.value = initial;
    }

    public synchronized void acquire() {
        value--;
        if (value < 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("operation interrupted." + e.getMessage());
            }
        }
    }

    public synchronized void release() {
        value++;
        if (value <= 0) {
            notify();
        }
    }
}
