package mytest;

/**
 * @author:qjj
 * @create: 2024-01-02 10:32
 * @Description:
 */

public class RealSubject implements Subject {

    @Override
    public void request() {
        System.out.println("From real subject.");
    }

}
