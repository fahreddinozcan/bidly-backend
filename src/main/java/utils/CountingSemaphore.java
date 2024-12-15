package utils;

// This is basically the CountingSemaphore implementation from the slides.
public class CountingSemaphore {
    int value;
    public CountingSemaphore(int initValue) {
        this.value = initValue;
    }

    public synchronized void P() {
        while (value == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        value--;
    }

    public synchronized void V() {
        value ++;
        notify();
    }
}
