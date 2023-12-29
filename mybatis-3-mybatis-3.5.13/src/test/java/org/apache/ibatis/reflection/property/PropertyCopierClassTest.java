//package org.apache.ibatis.reflection.property;
//
//import org.apache.ibatis.reflection.typeparam.Calculator;
//
//import java.time.LocalTime;
//
///**
// * @author:qjj
// * @create: 2023-12-29 09:38
// * @Description: 测试Bean反射复制
// */
//
//public class PropertyCopierClassTest {
//
//    @SneakyThrows
//    public static void main(String[] args) {
//        // 测试Bean反射复制
//        Calculator calculator = new Calculator();
//        calculator.setId(1);
//        Calculator calculator1 = new Calculator();
//        StopWatch stopWatch = new StopWatch("PropertyCopier");
//        stopWatch.start("Mybaits");
//        for (int i = 0; i < 100000; i++) {
//            PropertyCopier.copyBeanProperties(Calculator.class, calculator, calculator1);
//        }
//        stopWatch.stop();
//        stopWatch.start("hutool");
//        for (int i = 0; i < 100000; i++) {
//            BeanUtil.copyProperties(calculator, calculator1);
//        }
//        stopWatch.stop();
//        stopWatch.start("BeanUtils");
//        for (int i = 0; i < 100000; i++) {
//            BeanUtils.copyProperties(calculator, calculator1);
//        }
//        stopWatch.stop();
//        System.out.println(stopWatch.prettyPrint());
//    }
//}
