import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.Queue;

public class ServiceStationGUI extends JFrame {

    private JTextArea logArea;
    private JLabel[] pumpLabels;
    private JPanel queuePanel;

    // Shared resources
    private Queue<Car> queue;
    private Semaphore mutex, empty, full, pumps;

    public ServiceStationGUI() {
        setTitle("🚗 Car Wash & Gas Station Simulation");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 🎨 Make background cleaner and add padding
        getContentPane().setBackground(new Color(36, 36, 36));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        setLayout(new BorderLayout(10, 10));

        // Queue panel
        queuePanel = new JPanel();
        queuePanel.setBackground(new Color(250, 250, 250));
        queuePanel.setBorder(BorderFactory.createTitledBorder("Waiting Queue"));
        queuePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        add(queuePanel, BorderLayout.NORTH);

        // Pump status panel
        JPanel pumpPanel = new JPanel();
        pumpPanel.setBackground(new Color(250, 250, 250));
        pumpPanel.setBorder(BorderFactory.createTitledBorder("Service Bays"));
        pumpPanel.setLayout(new GridLayout(1, 3, 10, 10));
        add(pumpPanel, BorderLayout.CENTER);

        pumpLabels = new JLabel[3];
        for (int i = 0; i < 3; i++) {
            pumpLabels[i] = new JLabel("Pump " + (i + 1) + " - Free", SwingConstants.CENTER);
            pumpLabels[i].setOpaque(true);
            pumpLabels[i].setBackground(new Color(102, 187, 106)); // green
            pumpLabels[i].setForeground(Color.BLACK);
            pumpLabels[i].setFont(new Font("Segoe UI Semibold", Font.BOLD, 14)); // font
            pumpPanel.add(pumpLabels[i]);
        }

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12)); // font
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Activity Log"));
        scrollPane.setPreferredSize(new Dimension(600, 150)); // fixed height
        add(scrollPane, BorderLayout.SOUTH);

        setVisible(true);

        // Start the simulation
        new Thread(new Runnable() {
            public void run() {
                startSimulation();
            }
        }).start();
    }

    private void startSimulation() {
        int waitingAreaSize = 5;
        int numPumps = 3;

        queue = new LinkedList<>();
        mutex = new Semaphore(1);
        empty = new Semaphore(waitingAreaSize);
        full = new Semaphore(0);
        pumps = new Semaphore(numPumps);

        log(" *Car Wash & Gas Station Simulation (GUI Mode)*");
        log("Waiting Area Capacity: " + waitingAreaSize);
        log("Number of Service Bays: " + numPumps);
        log("----------------------------------------");

        // Start pumps
        for (int i = 1; i <= numPumps; i++) {
            Pump pump = new Pump(i, queue, mutex, empty, full, pumps, this);
            pump.start();
        }

        // Start cars
        try {
            for (int i = 1; i <= 5; i++) {
                Car car = new Car(i, queue, mutex, empty, full, 4000 + i * 500, this);
                car.start();
                Thread.sleep(700);
            }

            Thread.sleep(20000);
            log("----------------------------------------");
            log("All cars processed; simulation ends.");

        } catch (InterruptedException e) {
            log("Simulation interrupted.");
        }
    }

    // GUI helper methods
    public synchronized void log(String message) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                logArea.append(message + "\n");
            }
        });
    }

    public synchronized void updatePumpStatus(final int pumpId, final String text, final Color color) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                pumpLabels[pumpId - 1].setText(text);
                pumpLabels[pumpId - 1].setBackground(color);
            }
        });
    }

    public synchronized void updateQueueDisplay() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                queuePanel.removeAll();
                for (Car car : queue) {
                    JLabel carLabel = new JLabel("Car " + car.getCarIdName());
                    carLabel.setOpaque(true);
                    carLabel.setBackground(new Color(100, 181, 246)); // soft blue
                    carLabel.setForeground(Color.WHITE);
                    carLabel.setFont(new Font("Segoe UI", Font.BOLD, 13)); // font
                    carLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                    carLabel.setPreferredSize(new Dimension(60, 25));
                    carLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    queuePanel.add(carLabel);
                }
                queuePanel.revalidate();
                queuePanel.repaint();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ServiceStationGUI();
            }
        });
    }
}


// =================== Supporting Classes =================== //

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
    private final long serviceTime;
    private final ServiceStationGUI gui;

    public Car(int carId, Queue<Car> queue, Semaphore mutex, Semaphore empty, Semaphore full, long fixedServiceTime, ServiceStationGUI gui) {
        this.carId = carId;
        this.queue = queue;
        this.mutex = mutex;
        this.empty = empty;
        this.full = full;
        this.serviceTime = fixedServiceTime;
        this.gui = gui;
        this.setName("C" + this.carId);
    }

    public void run() {
        gui.log(getName() + " arrived");
        empty.acquire();

        mutex.acquire();
        queue.add(this);
        gui.log(getName() + " entered waiting queue");
        gui.updateQueueDisplay();
        mutex.release();

        full.release();
    }

    public String getCarIdName() {
        return this.getName();
    }

    public long getServiceTime() {
        return this.serviceTime;
    }
}

class Pump extends Thread {

    private final int pumpId;
    private final Queue<Car> queue;
    private final Semaphore mutex;
    private final Semaphore empty;
    private final Semaphore full;
    private final Semaphore pumps;
    private final ServiceStationGUI gui;

    public Pump(int pumpId, Queue<Car> queue, Semaphore mutex, Semaphore empty, Semaphore full, Semaphore pumps, ServiceStationGUI gui) {
        this.pumpId = pumpId;
        this.queue = queue;
        this.mutex = mutex;
        this.empty = empty;
        this.full = full;
        this.pumps = pumps;
        this.gui = gui;
        this.setName("Pump " + pumpId);
    }

    public void run() {
        while (true) {
            Car car = null;
            try {
                pumps.acquire();
                full.acquire();

                mutex.acquire();
                car = queue.poll();
                gui.updateQueueDisplay();
                mutex.release();

                empty.release();

                if (car != null) {
                    String carName = car.getCarIdName();
                    gui.log(getName() + ": " + carName + " Occupied");
                    gui.updatePumpStatus(pumpId, "Pump " + pumpId + " - Busy", new Color(229, 57, 53)); // red

                    gui.log(getName() + ": " + carName + " begins service");
                    Thread.sleep(car.getServiceTime());

                    gui.log(getName() + ": " + carName + " finishes service");
                    gui.updatePumpStatus(pumpId, "Pump " + pumpId + " - Free", new Color(102, 187, 106)); // restore green
                }

                pumps.release();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (car != null) pumps.release();
                break;
            }
        }
    }
}
