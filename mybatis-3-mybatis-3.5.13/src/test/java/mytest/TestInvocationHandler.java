package mytest;

import net.sf.cglib.proxy.InvocationHandler;
import net.sf.cglib.proxy.Proxy;

import java.lang.reflect.Method;

/**
 * @author:qjj
 * @create: 2024-01-02 10:25
 * @Description:
 */

public class TestInvocationHandler implements InvocationHandler {

    private Object target;

    public TestInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        Object result = method.invoke(target, objects);
        return result;
    }

    public Object getProxyObject() {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            target.getClass().getInterfaces(),this);
    }

    public static void main(String[] args) {
        RealSubject realSubject = new RealSubject();
        TestInvocationHandler testInvocationHandler = new TestInvocationHandler(realSubject);
        Subject subject = (Subject) testInvocationHandler.getProxyObject();
        subject.request();
    }
}
