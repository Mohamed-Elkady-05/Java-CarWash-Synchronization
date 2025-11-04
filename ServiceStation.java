import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class ServiceStation {

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);
        System.out.println(" *Car Wash & Gas Station Simulation*");
        System.out.print("Enter number of pumps (service bays): ");
        int numPumps = input.nextInt();
        System.out.print("Enter waiting area size (1–10): ");
        int waitingAreaSize = input.nextInt();

        // Validate queue size
        if (waitingAreaSize < 1 || waitingAreaSize > 10) {
            System.out.println("Waiting area size must be between 1 and 10");
            input.close();
            return;
        }
        input.close();

        // Shared Resources Initialization
        Queue<Car> queue = new LinkedList<>();     // Shared waiting queue
        Semaphore mutex = new Semaphore(1);        // For mutual exclusion
        Semaphore empty = new Semaphore(waitingAreaSize); // Available spaces
        Semaphore full = new Semaphore(0);         // Cars waiting
        Semaphore pumps = new Semaphore(numPumps); // Number of active pumps

        // Create and Start Pump Threads (Consumers)
        for (int i = 1; i <= numPumps; i++) {
            Pump pump = new Pump(i, queue, mutex, empty, full, pumps);
            pump.start();
        }

        // Create and Start Continuous Stream of Car Threads (Producers)
        int carId = 1;
        while (true) {
            try {
                Car car = new Car(carId++, queue, mutex, empty, full);
                car.start();

                Thread.sleep((int) (Math.random() * 1500 + 500));

            } catch (InterruptedException e) {
                System.out.println("Simulation interrupted");
                break;
            }
        }
    }
}
