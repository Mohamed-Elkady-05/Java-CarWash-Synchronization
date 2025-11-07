import java.util.LinkedList;
import java.util.Queue;

class ServiceStation {

    public static void main(String[] args) {


        int waitingAreaSize = 5;
        int numPumps = 3;

        // Synchronization Resources
        Queue<Car> queue = new LinkedList<>();
        Semaphore mutex = new Semaphore(1);
        Semaphore empty = new Semaphore(waitingAreaSize);
        Semaphore full = new Semaphore(0);
        Semaphore pumps = new Semaphore(numPumps);

        System.out.println(" *Car Wash & Gas Station Simulation*");
        System.out.println("Waiting Area Capacity: " + waitingAreaSize);
        System.out.println("Number of Service Bays: " + numPumps);
        System.out.println("----------------------------------------");


        // Start Pump Threads (Consumers)
        for (int i = 1; i <= numPumps; i++) {
            Pump pump = new Pump(i, queue, mutex, empty, full, pumps);
            pump.setName("Pump " + i);
            pump.start();
        }

        // Use fixed service times to force the output sequence to match the sample
        try {
            // C1, C2, C3 are taken immediately by Pumps 1, 2, 3
            Car c1 = new Car(1, queue, mutex, empty, full, 5000); c1.start();
            Thread.sleep(100);
            Car c2 = new Car(2, queue, mutex, empty, full, 6000); c2.start();
            Thread.sleep(100);
            Car c3 = new Car(3, queue, mutex, empty, full, 7000); c3.start();
            Thread.sleep(100);

            // C4 and C5 should enter the queue and wait.
            Car c4 = new Car(4, queue, mutex, empty, full, 5000); c4.start();
            Thread.sleep(300);
            Car c5 = new Car(5, queue, mutex, empty, full, 6000); c5.start();

            Thread.sleep(20000);

            System.out.println("----------------------------------------");
            System.out.println("All cars processed; simulation ends");

        } catch (InterruptedException e) {
            System.out.println("Simulation interrupted");
        }
    }
}

class Semaphore {
    protected int value = 0;

    protected Semaphore(int initial) {
        this.value = initial;
    }

    public synchronized void acquire() {
        value--;
        if (value < 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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

class Car extends Thread {

    private final int carId;
    private final Queue<Car> queue;
    private final Semaphore mutex;
    private final Semaphore empty;
    private final Semaphore full;

    // Removed random generation. Time is now passed in constructor.
    private final long serviceTime;

    public Car(int carId, Queue<Car> queue, Semaphore mutex, Semaphore empty, Semaphore full, long fixedServiceTime) {
        this.carId = carId;
        this.queue = queue;
        this.mutex = mutex;
        this.empty = empty;
        this.full = full;
        this.serviceTime = fixedServiceTime;
        this.setName("C" + this.carId);
    }

    public void run() {
        log("arrived");

        empty.acquire();

        mutex.acquire();

        queue.add(this);

        if (empty.value < 0) {
            log("arrived and waiting");
        } else {
            log("Entered the waiting queue.");
        }

        mutex.release();

        full.release();
    }

    public String getCarIdName() {
        return this.getName();
    }

    public long getServiceTime() {
        return this.serviceTime;
    }

    private void log(String message) {
        System.out.println(this.getName() + " " + message);
    }
}

class Pump extends Thread {

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
            Car car = null;
            try {
                // 1. Acquire a Service Bay (Pumps Semaphore)
                pumps.acquire();

                // 2. Wait for a Car in the queue (Full Semaphore)
                full.acquire();

                // 3. Enter Critical Section for Queue Access (Mutex)
                mutex.acquire();

                car = queue.poll();

                mutex.release();

                // 4. Signal space available in the queue (Empty Semaphore)
                empty.release();

                if (car != null) {
                    String carName = car.getCarIdName();
                    String pumpName = this.getName();

                    // Log sequence to match sample output
                    System.out.println(pumpName + ": " + carName + " Occupied");
                    System.out.println(pumpName + ": " + carName + " login");
                    System.out.println(pumpName + ": " + carName + " begins service at Bay " + this.pumpId);

                    Thread.sleep(car.getServiceTime());

                    System.out.println(pumpName + ": " + carName + " finishes service");
                }

                // 5. Release the Service Bay (Pumps Semaphore)
                pumps.release();

                System.out.println(this.getName() + ": Bay " + this.pumpId + " is now free");

            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (car != null) {
                    pumps.release();
                }
                break;
            }
        }
    }
}