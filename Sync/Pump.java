package Sync;

import java.util.Queue;

public class Pump extends Thread {

    private final int pumpId;
    private final Queue<Car> queue;
    private final Semaphore mutex;
    private final Semaphore empty;
    private final Semaphore full;
    private final Semaphore pumps;


    public Pump(int pumpId, Queue<Car> queue, Semaphore mutex, Semaphore empty, Semaphore full, Semaphore pumps) {
        this.pumpId = pumpId;
        this.queue = queue;
        this.mutex = mutex;
        this.empty = empty;
        this.full = full;
        this.pumps = pumps;
    }


    public void run() {
        while (true) {
            try {

                log("Waiting for a free service bay.");
                pumps.acquire();

                // If the queue is empty (full.count is 0), block here
                log("Waiting for a car to arrive in the queue.");
                full.acquire();

                mutex.acquire();

                Car car = queue.poll();

                mutex.release();

                empty.release();

                if (car != null) {
                    log("Started servicing " + car.getCarId());

                    Thread.sleep(car.getServiceTime());

                    log("Finished servicing " + car.getCarId());
                }

                pumps.release();
                log("Service bay is now free.");

            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log("Pump was interrupted and is shutting down.");
                break;
            }
        }
    }


    private void log(String message) {
        System.out.println("Pump " + this.pumpId + ": " + message);
    }
}