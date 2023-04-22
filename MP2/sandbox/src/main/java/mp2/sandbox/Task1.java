package mp2.sandbox;

public class Task1 implements Runnable {

    public boolean[] boolArr = null;

    public Task1(boolean[] boolArr) {
        this.boolArr = boolArr;
        boolArr[1] = true;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
            synchronized (boolArr) {
                boolArr[9] = true;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
