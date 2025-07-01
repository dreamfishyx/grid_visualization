package com.dreamfish.backend.parser;

import com.dreamfish.backend.parser.annotation.BinaryField;
import com.dreamfish.backend.parser.exception.ProtocolValidationException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Dream fish
 * @version 1.0
 * @description: BinaryField协议验证器
 * @date 2025/4/3 15:31
 */
public class BinaryFieldValidator {

    // 存储解析到的类及其嵌套类的有序的含有BinaryField注解的字段列表(用于后续参数解析)
    // private final Map<Class<?>, List<Field>> orderFieldsList = new HashMap<>();
    private final Map<Class<?>, List<Field>> orderFieldsList = new ConcurrentHashMap<>();

    // 最大嵌套深度
    private static final int MAX_NESTED_DEPTH = 5;

    /*
     * 验证协议类配置是否正确
     */
    public void validateProtocolClass(Class<?> clazz, int depth) {
        // 限制嵌套深度,防止死循环或者栈溢出
        if (depth > MAX_NESTED_DEPTH) {
            throw new ProtocolValidationException("Exceeded maximum nested depth: " + MAX_NESTED_DEPTH);
        }

        // 所验证类存在嵌套死循环
        if (orderFieldsList.containsKey(clazz)) {
            throw new ProtocolValidationException("The class " + clazz.getName() + " has a nested loop, please check the class definition.");
        }

        // 判断该类型是否是一个自定义对象类型: 这里直接排除java内置类型
        // 至于是不是自定义对象类型,后面通过 是否有BinaryField注解来判断
        if (clazz.getName().startsWith("java.") || clazz.isPrimitive()) {
            throw new ProtocolValidationException("@BinaryField annotation can only be used on custom object types, but got: " + clazz.getName());
        }

        LinkedList<Field> fields = new LinkedList<>();

        //遍历所有字段
        for (Field field : clazz.getDeclaredFields()) {
            //获取BinaryField注解
            BinaryField binaryField = field.getAnnotation(BinaryField.class);
            // BinaryField binaryField = field.getDeclaredAnnotation(BinaryField.class);
            if (binaryField != null) {
                //如果字段上有BinaryField注解，则将当前字段加入到fields列表中
                fields.add(field);
                //判断是否是嵌套注解
                if (binaryField.nested()) {
                    //如果是嵌套注解，则验证嵌套类
                    this.validateProtocolClass(field.getType(), depth + 1);
                } else {
                    // 判断是否是支持的字段类型
                    Class<?> type = field.getType();
                    if (!UniversalByteConverter.ALLOW_TYPE.contains(type)) {
                        // 如果不是基本数据类型,暂不支持,抛出异常
                        throw new ProtocolValidationException("Field type must be a primitive type or String, but" +
                                " got type " + type.getName() + " in " + clazz.getSimpleName() + "." + field.getName() +
                                ". Now supported types are: " + UniversalByteConverter.ALLOW_TYPE);
                    }
                }
            }
        }
        //如果类中没有BinaryField注解，则抛出异常
        if (fields.isEmpty()) {
            throw new ProtocolValidationException("No BinaryField annotations found in class: " + clazz.getName());
        }

        // 检查字段注解参数是否合理
        validateFieldOrders(fields, clazz);

    }

    public void validateFieldOrders(List<Field> fields, Class<?> clazz) {
        // 使用对字段按照优先级进行排序
        List<Field> orders = fields.stream()
                .sorted(Comparator.comparingInt(f -> f.getAnnotation(BinaryField.class).order()))
                .toList();

        // 保存当前类型的有序字段列表
        this.orderFieldsList.put(clazz, orders);

        //检查字段的order属性是否合理
        Field pre = null;
        for (Field field : orders) {
            BinaryField binaryField = field.getAnnotation(BinaryField.class);
            // 统一检查属性是否合理
            // 检查字段的order属性是否合理
            if (binaryField.order() < 0) {
                throw new ProtocolValidationException("Field order must be greater than or equal to 0. " +
                        "Class: " + clazz.getSimpleName() + ", Field: " + field.getName());
            }
            // 检查字段的length属性是否合理
            if (binaryField.length() < 0) {
                throw new ProtocolValidationException("Field length must be greater than or equal to 0. " +
                        "Class: " + clazz.getSimpleName() + ", Field: " + field.getName());
            }
            // 检查字段的order属性是否重复
            if (binaryField.order() == (pre != null ? pre.getAnnotation(BinaryField.class).order() : -1)) {
                throw new ProtocolValidationException("Field order must be unique. " +
                        "Class: " + clazz.getSimpleName() + ", Field: " + field.getName() + " and " + pre.getName());
            }

            // 检查字段的分组属性是否合理
            if (binaryField.groups() == null || binaryField.groups().length == 0) {
                throw new ProtocolValidationException("Field group cannot be null or empty. " +
                        "Class: " + clazz.getSimpleName() + ", Field: " + field.getName());
            }


            // 按类型检查字段的属性是否合理
            Class<?> type = field.getType();
            // 检查scale属性是否合理
            if (type == Float.class || type == float.class ||
                    type == Double.class || type == double.class ||
                    type == BigDecimal.class) {
                // scale属性必须大于0
                if (binaryField.scale() <= 0) {
                    throw new ProtocolValidationException("Field scale must be greater than  0. " +
                            "Class: " + clazz.getSimpleName() + ", Field: " + field.getName());
                }
                // 对于 bigDecimal类型,需要验证length属性
                if (type == BigDecimal.class) {
                    // length属性必须大于0
                    if (binaryField.length() == 0) {
                        throw new ProtocolValidationException("Field length must be greater than 0. " +
                                "Class: " + clazz.getSimpleName() + ", Field: " + field.getName());
                    }
                }
            } else {
                // 对于其他类型,scale属性必须为0
                if (binaryField.scale() != 0) {
                    throw new ProtocolValidationException("Field scale must be 0. " +
                            "Class: " + clazz.getSimpleName() + ", Field: " + field.getName());
                }
                // 对于 String类型,需要验证charset属性
                if (type == String.class) {
                    // 如果是字符串类型,则需要验证编码格式
                    String charset = binaryField.charset();
                    try {
                        // 验证编码格式
                        Charset.forName(charset);
                    } catch (IllegalArgumentException e) {
                        throw new ProtocolValidationException("Invalid charset: " + charset + " in " + clazz.getSimpleName() + "." + field.getName());
                    }
                    // 字符串长度必须大于0
                    if (binaryField.length() == 0) {
                        throw new ProtocolValidationException("String length must be greater than 0 which can not automatically deduce length). " +
                                "Class: " + clazz.getSimpleName() + ", Field: " + field.getName());
                    }
                }
            }

            // 更新上一个字段
            pre = field;
        }
    }

    /*
      获取有序含有BinaryField注解的字段
      @return 有序含有BinaryField注解的字段
     */
    public Map<Class<?>, List<Field>> getOrderFieldsList() {
        if (orderFieldsList.isEmpty()) {
            throw new ProtocolValidationException("No BinaryField annotations found in class");
        }
        return orderFieldsList;
    }
}

