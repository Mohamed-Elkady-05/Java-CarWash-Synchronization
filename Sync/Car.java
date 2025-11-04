package Sync;

import java.util.Queue;

public class Car extends Thread {

    private final int carId;
    private final Queue<Car> queue;
    private final Semaphore mutex;
    private final Semaphore empty;
    private final Semaphore full;

    private final long serviceTime;


    public Car(int carId, Queue<Car> queue, Semaphore mutex, Semaphore empty, Semaphore full) {
        this.carId = carId;
        this.queue = queue;
        this.mutex = mutex;
        this.empty = empty;
        this.full = full;

        this.serviceTime = (long) (Math.random() * 4000 + 3000);

        this.setName("Car " + this.carId);
    }


    public void run() {
        try {
            // If the queue is full (empty.count is 0), this blocks until a Pump signals 'empty'
            log("Arrived. Waiting for a spot in the queue.");
            empty.acquire();

            mutex.acquire();

            queue.add(this); // 'this' refers to this Car object
            log("Entered the waiting queue.");

            mutex.release();

            full.release();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log("Was interrupted and left the station.");
        }

    }


    public String getCarId() {
        return this.getName();
    }


    public long getServiceTime() {
        return this.serviceTime;
    }

    private void log(String message) {
        System.out.println(this.getName() + ": " + message);
    }
}